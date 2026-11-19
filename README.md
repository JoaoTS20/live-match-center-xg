# live-match-center-xg
Event-driven real-time football analytics and AI match narrator built with Java 21 (Spring Boot), Python (FastAPI), Apache Kafka, Docker, PostgreSQL, and Kubernetes.


# Live Match Center (xG & AI Narrator)

An event-driven, real-time football analytics and AI match commentary built with **Java 21 (Spring Boot)**, **Python (FastAPI)**, **Apache Kafka**, **Docker**, **PostgreSQL**, and **Kubernetes**.

## Architecture Overview

```text
                        ┌──────────────(SSE / Live Updates)──────────────┐
                        ▼                                                │
[ Frontend UI ] ──(REST)──► [ Match Service (Java 21) ] ──(SQL)──► [ PostgreSQL ]
                                     │       ▲
                              (match-events) │ (match-commentary)
                                     ▼       │
                                [ Apache Kafka ]
                                     │       ▲
                                     ▼       │
                           [ xG & AI Narrator (Python) ]

