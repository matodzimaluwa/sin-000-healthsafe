package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.mq.MqConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.IOException;
import java.time.Instant;

// Listens on the equipment-failure-queue and records every alert that arrives.
public class AlertConsumer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AlertStore store;
    private Connection connection;

    public AlertConsumer(AlertStore store) {
        this.store = store;
    }

    public void start() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            connection = factory.createConnection();
            // CLIENT_ACKNOWLEDGE: the broker keeps a message until we say we are done with it.
            // If this service stops before that, the message is delivered again later.
            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            Queue queue = session.createQueue(MqConfig.QUEUE);
            MessageConsumer consumer = session.createConsumer(queue);
            consumer.setMessageListener(this::handle);

            connection.start();
            System.out.println("Listening on queue " + MqConfig.QUEUE + " at " + MqConfig.BROKER_URL);
        } catch (JMSException e) {
            throw new IllegalStateException("Could not connect to ActiveMQ at " + MqConfig.BROKER_URL
                    + " - is the broker running? (docker compose up -d, in the common folder)", e);
        }
    }

    public void stop() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException e) {
            System.out.println("Could not close the ActiveMQ connection: " + e.getMessage());
        }
    }

    // Called once for every message that arrives
    private void handle(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                try {
                    JsonNode json = MAPPER.readTree(textMessage.getText());
                    EquipmentAlert alert = new EquipmentAlert(
                            json.path("wardId").asText(null),
                            json.path("equipment").asText(null),
                            Instant.now().toString());
                    store.add(alert);
                    System.out.println("EQUIPMENT FAILURE: " + alert.equipment + " on ward " + alert.wardId);
                } catch (IOException | IllegalArgumentException e) {
                    // A message we can never understand must not block the queue
                    System.out.println("Ignoring invalid equipment alert: " + e.getMessage());
                }
            } else {
                System.out.println("Ignoring a message that is not text");
            }
            // Tell the broker we are done, so it removes the message from the queue
            message.acknowledge();
        } catch (JMSException e) {
            // Not acknowledged, so the broker will deliver this message again
            System.out.println("Could not process an alert, it will be delivered again: " + e.getMessage());
        }
    }
}
