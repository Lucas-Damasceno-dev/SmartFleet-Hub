#!/bin/bash
# ============================================
# SmartFleet Hub - Debezium Connector Registration
# Register the PostgreSQL connector for CDC
# ============================================

set -e

DEBEZIUM_HOST="${DEBEZIUM_HOST:-localhost}"
DEBEZIUM_PORT="${DEBEZIUM_PORT:-8083}"
POSTGRES_HOST="${POSTGRES_HOST:-sf-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-smartfleet}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-smartfleet_secret_dev}"
POSTGRES_DB="${POSTGRES_DB:-smartfleet}"

echo "Waiting for Debezium Connect to be ready..."
until curl -s "http://${DEBEZIUM_HOST}:${DEBEZIUM_PORT}/connectors" > /dev/null 2>&1; do
    echo "Debezium Connect not ready yet, waiting..."
    sleep 5
done

echo "Debezium Connect is ready. Registering SmartFleet connector..."

curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
    "http://${DEBEZIUM_HOST}:${DEBEZIUM_PORT}/connectors/" \
    -d '{
      "name": "smartfleet-connector",
      "config": {
        "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
        "database.hostname": "'"${POSTGRES_HOST}"'",
        "database.port": "'"${POSTGRES_PORT}"'",
        "database.user": "'"${POSTGRES_USER}"'",
        "database.password": "'"${POSTGRES_PASSWORD}"'",
        "database.dbname": "'"${POSTGRES_DB}"'",
        "topic.prefix": "sf",
        "table.include.list": "public.outbox",
        "transforms": "outbox",
        "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
        "transforms.outbox.table.field.event.id": "id",
        "transforms.outbox.table.field.event.key": "aggregate_id",
        "transforms.outbox.table.field.event.type": "type",
        "transforms.outbox.table.field.event.payload": "payload",
        "transforms.outbox.route.topic.replacement": "outbox.event.${routedByValue}",
        "transforms.outbox.table.fields.additional.placement": "aggregate_type:header:aggregateType",
        "plugin.name": "pgoutput",
        "slot.name": "smartfleet_slot",
        "publication.name": "smartfleet_publication"
      }
    }'

echo ""
echo "Connector registration complete!"
echo "Check status: curl http://${DEBEZIUM_HOST}:${DEBEZIUM_PORT}/connectors/smartfleet-connector/status"
