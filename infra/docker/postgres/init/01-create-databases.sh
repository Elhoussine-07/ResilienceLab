#!/bin/bash

set -euo pipefail

create_service_database() {
  local db_name="$1"
  local db_user="$2"

  echo "Creating database '${db_name}' owned by '${db_user}'"

  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<EOSQL
CREATE ROLE ${db_user} LOGIN PASSWORD '${SERVICES_DB_PASSWORD}';
CREATE DATABASE ${db_name} OWNER ${db_user};
REVOKE ALL ON DATABASE ${db_name} FROM PUBLIC;
EOSQL
}

create_service_database catalog_db        catalog_user
create_service_database orders_db         order_user
create_service_database inventory_db      inventory_user
create_service_database payment_db        payment_user
create_service_database shipping_db       shipping_user
create_service_database resiliencelab_db  resiliencelab_user