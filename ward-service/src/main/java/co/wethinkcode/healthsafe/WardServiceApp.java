package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

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

        Javalin app = Javalin.create().start(7031);

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
