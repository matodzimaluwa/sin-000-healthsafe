# AlertLevelServiceApp

## Overview

Tracks the hospital Emergency Status (0-8, 8 = full Code Blue).

Part of the [HealthSafe](../README.md) project. Independent Maven module, no
parent pom.

REST: called by `staffing-service` (`../staffing-service`) to read the current
status when computing on-call schedules — see [Integration contracts](../README.md#integration-contracts)
in the root README for the endpoint shapes.

## Endpoints

Listens on port `7032`. The service starts at level `0` (normal) and remembers the level while it runs.

| Method | Path | Returns |
|---|---|---|
| GET | `/health` | `OK` |
| GET | `/alert-level` | The current Emergency Status, e.g. `{"level":5}` |
| PUT | `/alert-level` | Sets the Emergency Status. Body: `{"level":5}`. Answers with the new level. `400` if the level is outside 0-8, or if the body is broken or has no `level`. A rejected level leaves the old one in place. |

## Project structure

```
alert-level-service/
├── pom.xml
└── src/
    ├── main/java/co/wethinkcode/healthsafe/
    │   ├── AlertLevelServiceApp.java
    │   ├── AlertLevel.java
    │   └── LevelRequest.java
    └── test/java/co/wethinkcode/healthsafe/
        └── AlertLevelTest.java
```

`AlertLevel` holds the level and refuses anything outside 0-8. `LevelRequest` is the JSON body for `PUT`. It uses `Integer`, so a missing `level` shows up as `null` and is refused, instead of silently becoming `0`.

## Build

```
mvn package
```

## Run

```
java -jar target/alert-level-service.jar
```

Listens on port `7032`.

## Test

```
mvn test
```

5 tests in `AlertLevelTest`: the service starts at 0, every level from 0 to 8 is accepted, levels 9 and -1 are refused, and a refused level leaves the old level in place. Verify it's up:

```
curl http://localhost:7032/health   # -> OK
```

Try the endpoints:

```
curl http://localhost:7032/alert-level                                                           # -> {"level":0}
curl -X PUT -H "Content-Type: application/json" -d '{"level":5}' http://localhost:7032/alert-level   # -> {"level":5}
curl -i -X PUT -H "Content-Type: application/json" -d '{"level":9}' http://localhost:7032/alert-level # -> 400
```