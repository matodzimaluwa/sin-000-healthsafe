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
import java.util.ArrayList;
import java.util.List;

// Broadcasts staffing updates on the staffing-events-topic.
// Every service subscribed to the topic (ward-service) gets its own copy of each update.
public class StaffingPublisher {

    // Writes each event as JSON text.
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Connection connection;

    public void publish(List<StaffingEvent> events) throws JMSException, JsonProcessingException {
        // Build all the texts first, so a JSON problem is found before we touch the broker
        List<String> texts = new ArrayList<>();
        for (StaffingEvent event : events) {
            texts.add(MAPPER.writeValueAsString(event));
        }

        Session session = null;
        try {
            session = connection().createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(session.createTopic(MqConfig.TOPIC));

            // broker saves each update, so a subscriber that is briefly
            // down still receives it when it comes back
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            for (String text : texts) {
                producer.send(session.createTextMessage(text));
            }
        } catch (JMSException e) {
            // The connection may be broken: drop it so the next publish connects again
            reset();
            throw e;
        } finally {
            closeQuietly(session);
        }
    }

    public synchronized void stop() {
        reset();
    }

    // Connects on the first publish, not at startup, so staffing-service still runs when the broker is down
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