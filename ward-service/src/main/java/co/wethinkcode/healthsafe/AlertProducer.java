package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.mq.MqConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.util.Map;

// Puts equipment failure alerts on the equipment-failure-queue.
// equipment-alert-service collects them from there.
public class AlertProducer {

    // Writes the alert as JSON text.
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Connection connection;

    public void send(EquipmentFailure failure) throws JMSException, JsonProcessingException {
        String text = MAPPER.writeValueAsString(
                Map.of("wardId", failure.wardId, "equipment", failure.equipment));

        Session session = null;
        try {
            session = connection().createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(session.createQueue(MqConfig.QUEUE));

            // PERSISTENT: the broker saves the message to disk, so it survives a broker restart.
            // send() waits until the broker has the message, so a failed send is never silent.
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(session.createTextMessage(text));
        } catch (JMSException e) {
            // The connection may be broken: drop it so the next alert connects again
            reset();
            throw e;
        } finally {
            closeQuietly(session);
        }
    }

    public synchronized void stop() {
        reset();
    }

    // Connects on the first alert, not at startup, so ward-service still runs when the broker is down
    private synchronized Connection connection() throws JMSException {
        if (connection == null) {
            connection = new ActiveMQConnectionFactory(MqConfig.BROKER_URL).createConnection();
        }
        return connection;
    }

    private synchronized void reset() {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                System.out.println("Could not close the ActiveMQ connection: " + e.getMessage());
            }
            connection = null;
        }
    }

    private static void closeQuietly(Session session) {
        if (session != null) {
            try {
                session.close();
            } catch (JMSException e) {
                System.out.println("Could not close the ActiveMQ session: " + e.getMessage());
            }
        }
    }
}