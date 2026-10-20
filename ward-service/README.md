# WardServiceApp

## Overview

Provides lists of wards and departments.

Part of the [HealthSafe](../README.md) project. Independent Maven module, no
parent pom.

MQ: this service subscribes to the ActiveMQ topic `staffing-events-topic` — see [`../common/`](../common) — and publishes to the ActiveMQ queue `equipment-failure-queue` when it detects an equipment failure on one of its wards, consumed by [`../equipment-alert-service`](../equipment-alert-service). Broker URL, topic name, and queue name come from the common `co.wethinkcode.healthsafe.mq.MqConfig` class alongside it in this module.

REST: called by `staffing-service` (`../staffing-service`) and `alert-level-service` (`../alert-level-service`) — see [Integration contracts](../README.md#integration-contracts) in the root README for the endpoint shapes.

## Endpoints

Listens on port `7031`. At startup it fetches the cleaned wards from `ingestion-service` (`GET http://localhost:7030/wards`) and keeps its own copy.

| Method | Path | Returns |
|---|---|---|
| GET | `/health` | `OK` |
| GET | `/wards` | All wards, as a JSON list |
| GET | `/wards/{id}` | One ward, or `404` if the ward is unknown (`staffing-service` relies on the `404`). The id ignores capital letters and spaces. |
| GET | `/departments` | The distinct department names, A-Z |
| GET | `/wards/{id}/staffing` | The latest staffing update received from the topic, e.g. `{"wardId":"W-01","level":8,"doctorsOnCall":5}`. `404` if the ward is unknown, or if no update has arrived yet. |
| POST | `/wards/{id}/equipment-failure` | Reports an equipment failure. Body: `{"equipment":"ventilator"}`. `202` when the alert is on the queue, `404` for an unknown ward, `400` for a missing or broken body, `503` if the broker is not reachable. |

## Messaging

**Topic `staffing-events-topic` (consumer).** `StaffingListener` subscribes with a *durable* subscription (client id `ward-service`). If `ward-service` is down when `staffing-service` publishes, the broker keeps the updates and delivers them when it reconnects. Each update is stored in `StaffingBoard`, which keeps only the latest one per ward. If the broker is not running at startup, the service still runs and serves wards; only the staffing updates are unavailable.

**Queue `equipment-failure-queue` (producer).** `AlertProducer` sends each reported failure as a persistent message, `{"wardId":"W-01","equipment":"ventilator"}`, which `equipment-alert-service` collects. It connects to the broker on the first report, so `ward-service` starts without the broker.

## Project structure

```
ward-service/
├── pom.xml
└── src/
    ├── main/java/co/wethinkcode/healthsafe/
    │   ├── WardServiceApp.java
    │   ├── Ward.java
    │   ├── StaffingUpdate.java
    │   ├── StaffingBoard.java
    │   ├── StaffingListener.java
    │   ├── EquipmentFailure.java
    │   ├── EquipmentFailureRequest.java
    │   ├── AlertProducer.java
    │   └── mq/
    │       └── MqConfig.java
    └── test/java/co/wethinkcode/healthsafe/
        ├── StaffingUpdateTest.java
        ├── StaffingBoardTest.java
        └── EquipmentFailureTest.java
```

## Build

```
mvn package
```

## Run

Start the broker first (`docker compose up -d`, in `../common`), then `ingestion-service`, because `ward-service` fetches the wards from it at startup. Then:

```
java -jar target/ward-service.jar
```

On startup it prints `Subscribed to topic staffing-events-topic ...`.

## Test

```
mvn test
```

18 tests: `StaffingUpdateTest` and `StaffingBoardTest` check the staffing updates it receives, and `EquipmentFailureTest` checks the failures it reports. The code that talks to the broker is checked by hand. Verify it's up:

```
curl http://localhost:7031/health   # -> OK
```

Try the messaging end to end (with `staffing-service` and `alert-level-service` running):

```
curl -i -X POST http://localhost:7033/staffing/refresh              # -> 202, publishes to the topic
curl http://localhost:7031/wards/w-01/staffing                      # -> the update ward-service received
curl -i -X POST -H "Content-Type: application/json" -d '{"equipment":"ventilator"}' \
     http://localhost:7031/wards/w-01/equipment-failure             # -> 202, equipment-alert-service prints it
```