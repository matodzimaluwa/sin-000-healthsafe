package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class StaffingServiceApp {

    private static final String WARD_SERVICE_URL = "http://localhost:7031/wards/";
    private static final String ALERT_LEVEL_URL = "http://localhost:7032/alert-level";

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7033);

        app.get("/health", ctx -> ctx.result("OK"));

        // On-call schedule for one ward: how many doctors, given the current Emergency Status.
        app.get("/schedule/{wardId}", ctx -> {
            String wardId = ctx.pathParam("wardId").trim().toUpperCase();
            try {
                // ward-service answers 404 if the ward does not exist
                HttpResponse<String> wardResponse =
                        get(WARD_SERVICE_URL + URLEncoder.encode(wardId, StandardCharsets.UTF_8));
                if (wardResponse.statusCode() == 404) {
                    ctx.status(404).result("Ward not found: " + wardId);
                    return;
                }
                if (wardResponse.statusCode() != 200) {
                    ctx.status(502).result("ward-service answered with status " + wardResponse.statusCode());
                    return;
                }
                String department = MAPPER.readTree(wardResponse.body()).path("department").asText(null);

                // Get the hospital emergency status from alert-level-service
                HttpResponse<String> levelResponse = get(ALERT_LEVEL_URL);
                if (levelResponse.statusCode() != 200) {
                    ctx.status(502).result("alert-level-service answered with status " + levelResponse.statusCode());
                    return;
                }
                JsonNode levelNode = MAPPER.readTree(levelResponse.body()).path("level");
                if (!levelNode.isInt()) {
                    ctx.status(502).result("alert-level-service did not send a level");
                    return;
                }
                int level = levelNode.asInt();

                // 3. Work out the schedule and send it back.
                ctx.json(new Schedule(wardId, department, level, StaffingRules.doctorsOnCall(level)));

            } catch (IllegalArgumentException e) {
                ctx.status(502).result("alert-level-service sent a level outside 0-8");
            } catch (IOException e) {
                ctx.status(503).result("Could not reach ward-service or alert-level-service - are both running?");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ctx.status(503).result("Interrupted while contacting other services");
            }
        });
    }
    private static HttpResponse<String> get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
