\# Review Events Schema



This repository contains Avro schemas for all review-related domain events.



Used by:

\- review-rating-service (producer)

\- rating-aggregator-service (consumer)



\## Versioning

Schema version is stored in `version.txt`.



Follow semantic versioning:

\- MAJOR: breaking changes

\- MINOR: backward-compatible additions

\- PATCH: fixes



\## Usage

Add dependency:



implementation("com.itc.funkart:review-events-schema:<version>")



