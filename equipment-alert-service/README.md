# EquipmentAlertServiceApp

## Overview

Uses a Queue to guarantee delivery of critical medical equipment failure alerts.

Part of the [HealthSafe](../README.md) project — its alerting service.
Independent Maven module, no parent pom.

Mechanism: ActiveMQ Queue (guaranteed delivery)

Queue: `equipment-failure-queue`

- Producer: `ward-service` (`../ward-service`) — publishes when it detects an
  equipment failure on one of its wards.
- Consumer: this service.

Broker URL and queue name are shared via a common `co.wethinkcode.healthsafe.mq.MqConfig`
class (`BROKER_URL`, `QUEUE`). It's identical in every participating service's own
source tree — each service here is an independent Maven project with no shared
parent pom, so the common package is duplicated rather than imported from one place.

A queue (not a topic) is used deliberately: each failure alert must be delivered to
exactly one consumer and processed at least once, even if this service is briefly
down — unlike the broadcast `staffing-events-topic` in [`../common/`](../common),
where every subscriber gets every message.

## How it works

`AlertConsumer` listens on the queue. Each message is text in JSON form:

```json
{"wardId":"W-01","equipment":"ventilator"}
```

For every message it:

1. reads the ward and the equipment, and builds an `EquipmentAlert` (the ward id is tidied to upper case, and an alert with no ward or no equipment is refused),
2. saves the alert in the `AlertStore`,
3. prints `EQUIPMENT FAILURE: ventilator on ward W-01`,
4. only then acknowledges the message, so the broker removes it from the queue.

**Guaranteed delivery.** The session uses `CLIENT_ACKNOWLEDGE`, so the broker keeps a message until this service confirms it. If the service is stopped, messages wait in the queue and are delivered when it starts again. A message that can never be understood (broken JSON, a missing ward) is logged and acknowledged, so it does not block the ones behind it.

Alerts are kept in memory only, so `/alerts` is empty again after a restart.

## Endpoints

Listens on port `7034`.

| Method | Path | Returns |
|---|---|---|
| GET | `/health` | `OK` |
| GET | `/alerts` | Every alert received so far, oldest first, e.g. `[{"wardId":"W-01","equipment":"ventilator","receivedAt":"2026-10-14T08:32:43Z"}]` |

## Project structure

```
equipment-alert-service/
├── pom.xml
└── src/
    ├── main/java/co/wethinkcode/healthsafe/
    │   ├── EquipmentAlertServiceApp.java
    │   ├── AlertConsumer.java
    │   ├── AlertStore.java
    │   ├── EquipmentAlert.java
    │   └── mq/
    │       └── MqConfig.java
    └── test/java/co/wethinkcode/healthsafe/
        ├── EquipmentAlertTest.java
        └── AlertStoreTest.java
```

## Build

```
mvn package
```

## Run

Start the broker first (`docker compose up -d`, in `../common`). If the broker is not running, this service stops at startup with a message saying so. Then:

```
java -jar target/equipment-alert-service.jar
```

It prints `Listening on queue equipment-failure-queue at tcp://localhost:61616`.

## Test

```
mvn test
```

10 tests: `EquipmentAlertTest` (7) checks that alerts are tidied and that missing wards or equipment are refused, and `AlertStoreTest` (3) checks the order of the alerts and that the list handed out is a copy. The code that talks to the broker is checked by hand. Verify it's up:

```
curl http://localhost:7034/health   # -> OK
```

Try the whole path with `ward-service` running:

```
curl -i -X POST -H "Content-Type: application/json" -d '{"equipment":"ventilator"}' \
     http://localhost:7031/wards/w-01/equipment-failure   # -> 202, this service prints the alert
curl http://localhost:7034/alerts                          # -> the alert
```

Check the guaranteed delivery: stop this service, report a failure through `ward-service`, then start this service again. The alert is delivered straight away.