# IngestionServiceApp

## Overview

Parses and cleans `wards-outdated.csv`, a messy legacy export of wards, wings, and specialist departments data, and is the
first stop in the HealthSafe pipeline. Independent Maven module, no parent pom.

Part of the [HealthSafe](../README.md) project.

REST: exposes the cleaned records for `ward-service` (`../ward-service`) to
consume — see [Integration contracts](../README.md#integration-contracts) in the
root README for the endpoint shape.

## Example: one row cleaned

Input (`wards-outdated.csv`, row 6):

```
w-05,east wing ,PAEDIATRICS,five
```

Expected shape after cleaning (exact field names are up to you — this illustrates
the *kind* of transform expected, not a fixed schema to match exactly):

```json
{
  "wardId": "W-05",
  "wing": "East Wing",
  "department": "Paediatrics",
  "bedsAvailable": null,
  "notes": "bedsAvailable was non-numeric ('five') — flagged for follow-up"
}
```

Note this row is also a near-duplicate of `W-05` two rows above it (same real ward,
different ID casing and field values) — deciding how to merge or flag duplicates
like this is part of the exercise.

**What this service does with it:** the two `W-05` rows are merged into one record. The first row's values are kept (`East Wing`, `Paediatrics`, `5` beds), and `notes` says `Merged duplicate rows for W-05`. See [Duplicates](#duplicates).

## Known data issues

`wards-outdated.csv` is deliberately messy — cleaning it is the point of this service. Look
out for (and handle) at least:

- **Inconsistent casing** in IDs, names, and status/category values (`Active` /
  `active` / `ACTIVE`)
- **Padding** — leading/trailing spaces, and the occasional double space, inside
  fields
- **Duplicate records** for the same real-world entity, written with a different ID
  casing/format and/or slightly different field values
- **Inconsistent date formats** (`YYYY-MM-DD`, `MM/DD/YYYY`, `DD-MM-YYYY`, one- and
  two-digit months/days) and outright invalid dates
- **Missing / placeholder values** — blank fields, `N/A`, `n/a`, `TBD`, `unknown`,
  `-`, `NaN`
- **Invalid or non-numeric values** in numeric columns (negative counts, spelled-out
  numbers, unrealistic values)
- **Inconsistent boolean/flag representations** (`Y`/`N`, `yes`/`no`, `1`/`0`,
  `true`/`FALSE`)
- **Naming/spelling variants** for the same thing (e.g. regional spelling
  differences, synonyms)

## How the data is cleaned

`WardDataCleaner.loadAndClean(...)` reads the CSV from the classpath and cleans every row. A value that cannot be used becomes `null`, and `notes` says why, so nothing is dropped silently.

| Issue in the CSV | How it is handled |
|---|---|
| Inconsistent casing (`w-02`, `east wing`, `PAEDIATRICS`) | Ward ids are upper case; wings and departments are capitalised word by word |
| Padding and double spaces (`"W-03 "`, `"South  Wing"`) | Trimmed, and runs of spaces collapsed to one |
| Spelling variants (`Paediatrics` / `Pediatrics`) | Both become `Paediatrics` |
| Missing wing (`W-08`) | `wing` is `null` and `notes` says `wing was missing` |
| Placeholder bed counts (`N/A`, `TBD`, `unknown`, `-`, `NaN`, blank) | `bedsAvailable` is `null` and `notes` says it was missing |
| Non-numeric bed counts (`five`, `full`) | `bedsAvailable` is `null` and `notes` says it was non-numeric |
| Negative bed counts (`-1`, `-2`) | `bedsAvailable` is `null` and `notes` says it was negative |
| Unrealistic bed counts (`2023`) | `bedsAvailable` is `null` and `notes` says it looked unrealistic (more than 100 beds in one ward) |
| Rows with fewer than 4 fields | Skipped, so one bad row never stops the load |

The CSV has only `ward_id`, `wing`, `department` and `beds_available`, so the date and boolean issues in the list above do not occur in this file.

### Duplicates

Rows with the same cleaned ward id are treated as the same ward and merged into one record. The first row's values are kept, any gap (a `null`) is filled from the duplicate, and `notes` says `Merged duplicate rows for W-05`.

Known limitation: a warning that belonged only to the duplicate row (for example the `five` in `W-05`) is not copied into the merged record.

## Endpoints

Listens on port `7030`.

| Method | Path | Returns |
|---|---|---|
| GET | `/health` | `OK` |
| GET | `/wards` | The cleaned records as a JSON list, one per ward (17 for the supplied CSV) |

Example record:

```json
{"wardId":"W-08","wing":null,"department":"Oncology","bedsAvailable":4,"notes":"wing was missing"}
```

## Project structure

```
ingestion-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/co/wethinkcode/healthsafe/
    │   │   ├── IngestionServiceApp.java
    │   │   ├── WardDataCleaner.java
    │   │   └── WardRecord.java
    │   └── resources/wards-outdated.csv
    └── test/
        ├── java/co/wethinkcode/healthsafe/
        │   ├── WardDataCleanerTest.java
        │   └── WardRecordTest.java
        └── resources/test-wards.csv
```

## Build

```
mvn package
```

## Run

```
java -jar target/ingestion-service.jar
```

Listens on port `7030`. Start this service first: `ward-service` fetches the cleaned wards from it at startup.

## Test

```
mvn test
```

`WardDataCleanerTest` checks the cleaning rules against `src/test/resources/test-wards.csv`: casing, padding, the missing wing, the `Paediatrics` spelling, placeholder and invalid bed counts, good counts being left alone, and duplicates being merged. `WardRecordTest` checks the record class. Verify it's up:

```
curl http://localhost:7030/health   # -> OK
curl http://localhost:7030/wards    # -> the cleaned records
```

## Status

Stage 1 (ingestion) is done.