#!/bin/bash
# Legt beim ersten Start von PostgreSQL (leeres Datenverzeichnis) die Datenbanken
# für Keycloak und Luna mit je einem eigenen Benutzer an.
# Die Schemas der einzelnen Luna-Backends legen deren Flyway-Migrationen an.
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
