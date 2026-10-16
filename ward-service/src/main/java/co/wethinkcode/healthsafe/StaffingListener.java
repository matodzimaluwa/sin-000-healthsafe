package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.mq.MqConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.io.IOException;

// Subscribes to the staffing-events-topic and keeps the latest update for each ward.
public class StaffingListener {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // A durable subscription is identified by these two names. While ward-service is down,
    // the broker keeps its updates and hands them over when it connects again.
    private static final String CLIENT_ID = "ward-service";
    private static final String SUBSCRIPTION_NAME = "ward-service-staffing";

    private final StaffingBoard board;
    private Connection connection;

    public StaffingListener(StaffingBoard board) {
        this.board = board;
    }

    // If the broker is not running, ward-service still works, only staffing updates are unavailable
    public void start() {
        try {
            connection = new ActiveMQConnectionFactory(MqConfig.BROKER_URL).createConnection();
            connection.setClientID(CLIENT_ID);

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            MessageConsumer subscriber = session.createDurableSubscriber(topic, SUBSCRIPTION_NAME);
            subscriber.setMessageListener(this::handle);

            connection.start();
            System.out.println("Subscribed to topic " + MqConfig.TOPIC + " at " + MqConfig.BROKER_URL);
        } catch (JMSException e) {
            stop();
            System.out.println("Could not subscribe to topic " + MqConfig.TOPIC
                    + " - staffing updates are unavailable (" + e.getMessage() + "). "
                    + "Is the broker running? (docker compose up -d, in the common folder)");
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

    // Called once for every update that arrives
    private void handle(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                try {
                    JsonNode json = MAPPER.readTree(textMessage.getText());
                    // -1 when a field is missing, which StaffingUpdate refuses
                    StaffingUpdate update = new StaffingUpdate(
                            json.path("wardId").asText(null),
                            json.path("level").asInt(-1),
                            json.path("doctorsOnCall").asInt(-1));
                    board.put(update);
                    System.out.println("STAFFING UPDATE: ward " + update.wardId + " has "
                            + update.doctorsOnCall + " doctors on call at level " + update.level);
                } catch (IOException | IllegalArgumentException e) {
                    // An update we cannot understand must not stop the ones that follow
                    System.out.println("Ignoring invalid staffing update: " + e.getMessage());
                }
            } else {
                System.out.println("Ignoring a message that is not text");
            }
        } catch (JMSException e) {
            System.out.println("Could not read a staffing update: " + e.getMessage());
        }
    }
}