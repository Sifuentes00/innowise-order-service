#!/bin/bash
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER order_service_user WITH PASSWORD 'order_service_password';
    GRANT CONNECT ON DATABASE innowise_order_service TO order_service_user;
    GRANT USAGE ON SCHEMA public TO order_service_user;
    GRANT CREATE ON SCHEMA public TO order_service_user;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO order_service_user;
    GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO order_service_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO order_service_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO order_service_user;
EOSQL
