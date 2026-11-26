#!/bin/bash
# Creates or updates the SCRAM users and ACLs of all services listed in LUNA_KAFKA_SERVICES
# and creates the topics they may read if they do not exist yet.
# Runs on every deploy that changes the kafka-init service and is idempotent.
# Removed services or topics are not cleaned up automatically.
set -euo pipefail

kafka_bin=/opt/kafka/bin
bootstrap=kafka:9094
client_config="$(mktemp)"
trap 'rm -f "$client_config"' EXIT

admin_password="$(< /run/secrets/kafka_admin_password)"
cat > "$client_config" <<EOF
security.protocol=SASL_SSL
sasl.mechanism=SCRAM-SHA-512
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="admin" password="${admin_password}";
ssl.truststore.type=PEM
ssl.truststore.location=/step/certs/root_ca.crt
EOF

echo "Waiting for Kafka ..."
until "$kafka_bin/kafka-topics.sh" --bootstrap-server "$bootstrap" --command-config "$client_config" --list > /dev/null 2>&1; do
  sleep 5
done

acl() {
  "$kafka_bin/kafka-acls.sh" --bootstrap-server "$bootstrap" --command-config "$client_config" --add --force "$@" > /dev/null
}

while IFS=: read -r service read_topics; do
  service="$(echo "$service" | xargs)"
  [[ -z "$service" ]] && continue

  password_file="/run/secrets/kafka_${service//-/_}_password"
  topic_prefix="luna_${service#luna-}_"
  topic_prefix="${topic_prefix//-/_}"

  echo "Configuring ${service} (writes ${topic_prefix}*) ..."
  "$kafka_bin/kafka-configs.sh" --bootstrap-server "$bootstrap" --command-config "$client_config" --alter \
    --entity-type users --entity-name "$service" \
    --add-config "SCRAM-SHA-512=[password=$(< "$password_file")]" > /dev/null

  acl --allow-principal "User:${service}" --operation Write --operation Create --operation Describe \
    --topic "$topic_prefix" --resource-pattern-type prefixed
  acl --allow-principal "User:${service}" --operation Read --group "$service"

  IFS=, read -ra topics <<< "$read_topics"
  for topic in "${topics[@]}"; do
    topic="$(echo "$topic" | xargs)"
    [[ -z "$topic" ]] && continue
    echo "  may read ${topic}"
    # Readers must not create foreign topics, so a consumer starting before the producer would find none.
    "$kafka_bin/kafka-topics.sh" --bootstrap-server "$bootstrap" --command-config "$client_config" \
      --create --if-not-exists --topic "$topic" > /dev/null
    acl --allow-principal "User:${service}" --operation Read --operation Describe --topic "$topic"
  done
done <<< "$LUNA_KAFKA_SERVICES"

echo "Kafka users and ACLs are up to date."
