package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import javax.jms.JMSException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;


public class WardServiceApp {

    private static final String INGESTION_URL = "http://localhost:7030/wards";

    public static void main(String[] args) {

        List<Ward> wards = fetchWards();

        // Every distinct department name, A-Z, built from that same list.
        Set<String> departments = wards.stream()
                .map(w -> w.department)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        // Sends equipment failures to the equipment-failure-queue. It only connects to the
        // broker when the first failure is reported.
        AlertProducer alertProducer = new AlertProducer();

        // Keeps the latest staffing update for each ward. The listener fills it from the staffing-events-topic.
        StaffingBoard staffingBoard = new StaffingBoard();
        StaffingListener staffingListener = new StaffingListener(staffingBoard);


        Javalin app = Javalin.create().start(7031);

        // Subscribe to the staffing-events-topic, the web server is already up, so a missing broker does not stop it.
        staffingListener.start();

        app.get("/health", ctx -> ctx.result("OK"));

        // List of all wards
        app.get("/wards", ctx -> ctx.json(wards));

        // One ward by id, or 404 if we don't have it (staffing-service relies on the 404)
        app.get("/wards/{id}", ctx -> {
            String id = ctx.pathParam("id").trim().toUpperCase();
            wards.stream()
                    .filter(w -> w.wardId.equals(id))
                    .findFirst()
                    .ifPresentOrElse(
                            w -> ctx.json(w),
                            () -> ctx.status(404).result("Ward not found: " + id));
        });

        // List of departments
        app.get("/departments", ctx -> ctx.json(departments));

        // The latest staffing update this ward received from the staffing-events-topic
        app.get("/wards/{id}/staffing", ctx -> {
            String id = ctx.pathParam("id").trim().toUpperCase();

            boolean wardExists = wards.stream().anyMatch(w -> w.wardId.equals(id));
            if (!wardExists) {
                ctx.status(404).result("Ward not found: " + id);
                return;
            }
            staffingBoard.find(id).ifPresentOrElse(
                    update -> ctx.json(update),
                    () -> ctx.status(404).result("No staffing update received yet for ward " + id));
        });

        // Report an equipment failure on a ward. Body.
        // The alert goes onto the equipment-failure-queue for equipment-alert-service.
        app.post("/wards/{id}/equipment-failure", ctx -> {
            String id = ctx.pathParam("id").trim().toUpperCase();

            // 404 if it is not one of our wards
            boolean wardExists = wards.stream().anyMatch(w -> w.wardId.equals(id));
            if (!wardExists) {
                ctx.status(404).result("Ward not found: " + id);
                return;
            }

            // 400 if the body is broken or has no equipment
            EquipmentFailure failure;
            try {
                EquipmentFailureRequest request = ctx.bodyAsClass(EquipmentFailureRequest.class);
                failure = new EquipmentFailure(id, request.equipment);
            } catch (IllegalArgumentException e) {
                ctx.status(400).result(e.getMessage());
                return;
            } catch (Exception e) {
                ctx.status(400).result("body must look like {\"equipment\": \"ventilator\"}");
                return;
            }

            try {
                alertProducer.send(failure);
                // 202 Accepted: the alert is on the queue, it has not been handled yet
                ctx.status(202).result("Equipment failure reported for ward " + id);
            } catch (JMSException e) {
                ctx.status(503).result("Could not reach the alert queue - is the ActiveMQ broker running?");
            } catch (JsonProcessingException e) {
                ctx.status(500).result("Could not build the alert message");
            }
        });

        // Close the broker connections when the service is stopped (Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            alertProducer.stop();
            staffingListener.stop();
        }));
    }

    private static List<Ward> fetchWards() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(INGESTION_URL)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "ingestion-service answered with status " + response.statusCode());
            }

            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(response.body(), new TypeReference<List<Ward>>() {});
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not reach ingestion-service at " + INGESTION_URL
                            + " - start it first (java -jar target/ingestion-service.jar)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while contacting ingestion-service", e);
        }
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
// MQ TODO: publishes to ActiveMQ queue MqConfig.QUEUE when it detects an equipment failure on one of its wards.
