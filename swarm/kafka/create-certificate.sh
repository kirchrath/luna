#!/bin/bash
# Requests the TLS certificate for Kafka (valid for one year) from step-ca and stores it as the
# versioned Docker secrets kafka_keystore_v<version> and kafka_keystore_password_v<version>.
# On first use it adds the step-ca provisioner "kafka" (max. one year) and restarts step-ca.
#
# Usage on the server (stack "luna" with step-ca running):
#   swarm/kafka/create-certificate.sh <version>
set -euo pipefail

version="${1:?usage: $0 <version>}"
step_ca_service=luna_step-ca
ca_args=(--ca-url https://localhost:9000 --root /home/step/certs/root_ca.crt)

step_ca_container() {
  docker ps --quiet --filter "name=${step_ca_service}." --filter health=healthy | head -n 1
}

wait_for_step_ca() {
  local container=""
  for _ in $(seq 1 60); do
    container="$(step_ca_container)"
    [[ -n "$container" ]] && break
    sleep 2
  done
  [[ -n "$container" ]] || { echo "step-ca is not running (service ${step_ca_service})." >&2; exit 1; }
  echo "$container"
}

container="$(wait_for_step_ca)"

if ! docker exec "$container" step ca provisioner list "${ca_args[@]}" | grep -q '"name": "kafka"'; then
  echo "Adding step-ca provisioner \"kafka\" ..."
  docker exec "$container" step ca provisioner add kafka --type JWK --create \
    --password-file /run/secrets/step_ca_password \
    --x509-default-dur 8760h --x509-max-dur 8760h
  docker service update --force --quiet "$step_ca_service" > /dev/null
  container="$(wait_for_step_ca)"
fi

workdir="$(docker exec "$container" mktemp -d)"
keystore_password="$(openssl rand -base64 24 | tr -d '\n')"

echo "Requesting certificate ..."
docker exec "$container" step ca certificate kafka "$workdir/kafka.crt" "$workdir/kafka.key" "${ca_args[@]}" \
  --provisioner kafka --provisioner-password-file /run/secrets/step_ca_password \
  --san kafka --san localhost --san luna.internal --not-after 8760h
printf '%s' "$keystore_password" | docker exec -i "$container" sh -c "cat > '$workdir/password'"
docker exec "$container" step certificate p12 "$workdir/kafka.p12" "$workdir/kafka.crt" "$workdir/kafka.key" \
  --password-file "$workdir/password"

docker exec "$container" cat "$workdir/kafka.p12" | docker secret create "kafka_keystore_v${version}" - > /dev/null
printf '%s' "$keystore_password" | docker secret create "kafka_keystore_password_v${version}" - > /dev/null
docker exec "$container" sh -c "cd '$workdir' && rm kafka.crt kafka.key kafka.p12 password && cd / && rmdir '$workdir'"

echo "Created secrets kafka_keystore_v${version} and kafka_keystore_password_v${version}."
echo "Deploy with: LUNA_KAFKA_CERT_VERSION=${version} docker stack deploy -c swarm/infrastructure.yml -c swarm/kafka.yml luna"
