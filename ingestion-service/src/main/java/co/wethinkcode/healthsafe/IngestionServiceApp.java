package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.io.IOException;
import java.util.List;


public class IngestionServiceApp {

    public static void main(String[] args) throws IOException{
        String csvPath = "wards-outdated.csv";
        List<WardRecord> wards = WardDataCleaner.loadAndClean(csvPath);

        Javalin app = Javalin.create().start(7030);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/wards", ctx -> ctx.json(wards));

    }
}
