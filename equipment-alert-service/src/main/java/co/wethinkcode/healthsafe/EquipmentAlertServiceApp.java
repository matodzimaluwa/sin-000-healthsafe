package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

public class EquipmentAlertServiceApp {

    public static void main(String[] args) {
        AlertStore store = new AlertStore();

        // Start listening to the queue first. If the broker is not running, stop here with a clear message.
        AlertConsumer consumer = new AlertConsumer(store);
        consumer.start();

        Javalin app = Javalin.create().start(7034);

        app.get("/health", ctx -> ctx.result("OK"));

        // Every alert received so far, oldest first
        app.get("/alerts", ctx -> ctx.json(store.all()));

        // Close the broker connection when the service is stopped (Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::stop));
    }
}

// MQ TODO: consumes ActiveMQ queue MqConfig.QUEUE at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
// Producer: ward-service publishes here when it detects an equipment failure on one of its wards.
