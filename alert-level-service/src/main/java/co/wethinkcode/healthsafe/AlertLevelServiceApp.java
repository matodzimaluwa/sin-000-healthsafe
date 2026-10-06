package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.util.Map;
public class AlertLevelServiceApp {

    public static void main(String[] args) {
        AlertLevel alertLevel = new AlertLevel();

        Javalin app = Javalin.create().start(7032);

        app.get("/health", ctx -> ctx.result("OK"));

        // Current Emergency Status. staffing-service calls this: -> { "level": 0-8 }
        app.get("/alert-level", ctx -> ctx.json(Map.of("level", alertLevel.get())));


        // Change the Emergency Status. Body: {"level": 5}. Anything outside 0-8 gets a 400.
        app.put("/alert-level", ctx -> {
            try {
                LevelRequest request = ctx.bodyAsClass(LevelRequest.class);
                if (request.level == null) {
                    ctx.status(400).result("body must look like {\"level\": 5}");
                    return;
                }
                alertLevel.set(request.level);
                ctx.json(Map.of("level", alertLevel.get()));
            } catch (IllegalArgumentException e) {
                ctx.status(400).result(e.getMessage());
            } catch (Exception e) {
                ctx.status(400).result("body must look like {\"level\": 5}");
            }
        });
    }
}
