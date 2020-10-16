#!/usr/bin/env bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER fritakagp WITH PASSWORD 'fritakagp';
    CREATE DATABASE fritakagp_db;
    CREATE SCHEMA fritakagp;
    GRANT ALL PRIVILEGES ON DATABASE fritakagp_db TO fritakagp;
EOSQL

psql -v ON_ERROR_STOP=1 --username "fritakagp" --dbname "fritakagp_db" <<-EOSQL
    CREATE TABLE test (
                          id int unique not null,
                          data jsonb not null
    );

EOSQL

