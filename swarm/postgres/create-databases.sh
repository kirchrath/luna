#!/bin/bash
# Creates the databases for Keycloak and Luna, each with its own user, on the first start
# of PostgreSQL (empty data directory).
# The schemas of the Luna backends are created by their Flyway migrations.
set -euo pipefail

create_database() {
  local db="$1" user="$2" password_file="$3"
  local password
  password="$(< "$password_file")"

  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    -v db="$db" -v user="$user" -v password="$password" <<'SQL'
CREATE ROLE :"user" LOGIN PASSWORD :'password';
CREATE DATABASE :"db" OWNER :"user";
SQL
}

create_database keycloak keycloak /run/secrets/keycloak_db_password
create_database luna luna /run/secrets/luna_db_password
