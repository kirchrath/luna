#!/bin/bash
# Starts Kafka with SASL/SCRAM. The admin user has to exist before the broker accepts SCRAM logins,
# so on the very first start the storage is formatted here together with the admin credentials.
# Passwords come from Docker secrets; the image's own SSL setup only handles listeners named SSL.
set -euo pipefail

admin_password="$(< /run/secrets/kafka_admin_password)"

export KAFKA_SSL_KEYSTORE_PASSWORD KAFKA_SSL_KEY_PASSWORD
KAFKA_SSL_KEYSTORE_PASSWORD="$(< /run/secrets/kafka_keystore_password)"
KAFKA_SSL_KEY_PASSWORD="$KAFKA_SSL_KEYSTORE_PASSWORD"

export KAFKA_LISTENER_NAME_INTERNAL_SCRAM___SHA___512_SASL_JAAS_CONFIG="org.apache.kafka.common.security.scram.ScramLoginModule required username=\"admin\" password=\"${admin_password}\";"
export KAFKA_LISTENER_NAME_EXTERNAL_SCRAM___SHA___512_SASL_JAAS_CONFIG="org.apache.kafka.common.security.scram.ScramLoginModule required;"

if [[ ! -f "${KAFKA_LOG_DIRS}/meta.properties" ]]; then
  format_config="$(mktemp)"
  cat > "$format_config" <<EOF
node.id=${KAFKA_NODE_ID}
process.roles=${KAFKA_PROCESS_ROLES}
controller.quorum.voters=${KAFKA_CONTROLLER_QUORUM_VOTERS}
controller.listener.names=${KAFKA_CONTROLLER_LISTENER_NAMES}
listeners=PLAINTEXT://localhost:9092,CONTROLLER://localhost:9093
listener.security.protocol.map=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
log.dirs=${KAFKA_LOG_DIRS}
EOF
  /opt/kafka/bin/kafka-storage.sh format \
    --cluster-id "$CLUSTER_ID" \
    --config "$format_config" \
    --add-scram "SCRAM-SHA-512=[name=admin,password=${admin_password}]"
  rm "$format_config"
fi

exec /etc/kafka/docker/run
