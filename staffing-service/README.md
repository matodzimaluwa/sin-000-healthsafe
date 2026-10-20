# StaffingServiceApp

## Overview

Provides on-call schedules for doctors based on ward and status.

Part of the [HealthSafe](../README.md) project. Independent Maven module, no
parent pom.

MQ: this service publishes to the ActiveMQ topic `staffing-events-topic` — see [`../common/`](../common). Broker URL and topic name come from the common `co.wethinkcode.healthsafe.mq.MqConfig` class alongside it in this module.

REST: calls `ward-service` (`../ward-service`) to validate the ward and
`alert-level-service` (`../alert-level-service`) to read the current Emergency
Status before computing a schedule — see [Integration contracts](../README.md#integration-contracts)
in the root README for the endpoint shapes.

## Endpoints

Listens on port `7033`.

| Method | Path | Returns |
|---|---|---|
| GET | `/health` | `OK` |
| GET | `/schedule/{wardId}` | The on-call schedule for one ward, e.g. `{"wardId":"W-01","department":"Cardiology","level":8,"doctorsOnCall":5}`. `404` if `ward-service` does not know the ward, `502` if another service answers with an error, `503` if `ward-service` or `alert-level-service` cannot be reached. |
| POST | `/staffing/refresh` | Works out the schedule for every ward at the current Emergency Status and broadcasts one update per ward on the topic. `202` with `Published staffing updates for 17 wards at level 8`. `502` or `503` if another service or the broker cannot be reached. |

### How the schedule is worked out

`StaffingRules` turns the Emergency Status (0-8) into a number of doctors on call: 1 doctor at level 0, plus 1 more for every 2 levels.

| Level | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|---|
| Doctors | 1 | 1 | 2 | 2 | 3 | 3 | 4 | 4 | 5 |

A level outside 0-8 is refused. The README does not fix the rule, so this one is our choice.

## Messaging

`POST /staffing/refresh` asks `ward-service` for the list of wards and `alert-level-service` for the level, then `StaffingPublisher` broadcasts one persistent message per ward on `staffing-events-topic`:

```json
{"wardId":"W-01","level":8,"doctorsOnCall":5}
```

`ward-service` subscribes to the topic and keeps the latest update for each ward. The publisher connects to the broker on the first refresh, so `staffing-service` starts without the broker. Messages are persistent, so a subscriber that is briefly down still receives them when it reconnects.

## Project structure

```
staffing-service/
├── pom.xml
└── src/
    ├── main/java/co/wethinkcode/healthsafe/
    │   ├── StaffingServiceApp.java
    │   ├── StaffingRules.java
    │   ├── Schedule.java
    │   ├── StaffingEvent.java
    │   ├── StaffingPublisher.java
    │   └── mq/
    │       └── MqConfig.java
    └── test/java/co/wethinkcode/healthsafe/
        ├── StaffingRulesTest.java
        └── StaffingEventTest.java
```

## Build

```
mvn package
```

## Run

Start the broker first (`docker compose up -d`, in `../common`), then `ingestion-service`, `ward-service` and `alert-level-service`, which this service calls. Then:

```
java -jar target/staffing-service.jar
```

## Test

```
mvn test
```

12 tests: `StaffingRulesTest` checks the doctors for each level, and `StaffingEventTest` checks the updates that are broadcast. The code that talks to the broker is checked by hand. Verify it's up:

```
curl http://localhost:7033/health   # -> OK
```

Try the endpoints (with the other services running):

```
curl http://localhost:7033/schedule/W-01            # -> level and doctors on call
curl -i http://localhost:7033/schedule/W-99         # -> 404 Ward not found
curl -i -X POST http://localhost:7033/staffing/refresh   # -> 202, then ward-service prints the updates
```