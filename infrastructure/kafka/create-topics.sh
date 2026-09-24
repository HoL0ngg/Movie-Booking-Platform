#!/usr/bin/env bash
set -euo pipefail

bootstrap_server="${BOOTSTRAP_SERVER:-kafka:9092}"
partitions="${KAFKA_TOPIC_PARTITIONS:-3}"
event_retention_ms="${KAFKA_EVENT_RETENTION_MS:-604800000}"
quarantine_retention_ms="${KAFKA_QUARANTINE_RETENTION_MS:-1209600000}"
kafka_topics="/opt/kafka/bin/kafka-topics.sh"

create_topic() {
  local topic="$1"
  local retention_ms="$2"

  "${kafka_topics}" \
    --bootstrap-server "${bootstrap_server}" \
    --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions "${partitions}" \
    --replication-factor 1 \
    --config cleanup.policy=delete \
    --config "retention.ms=${retention_ms}"
}

create_topic "cinema.showtime-events.v1" "${event_retention_ms}"
create_topic "cinema.booking-events.v1" "${event_retention_ms}"
create_topic "cinema.payment-events.v1" "${event_retention_ms}"
create_topic "cinema.showtime-events.quarantine.v1" "${quarantine_retention_ms}"
create_topic "cinema.booking-events.quarantine.v1" "${quarantine_retention_ms}"
create_topic "cinema.payment-events.quarantine.v1" "${quarantine_retention_ms}"

"${kafka_topics}" --bootstrap-server "${bootstrap_server}" --list

