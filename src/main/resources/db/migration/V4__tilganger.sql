GRANT ALL PRIVILEGES ON TABLE "fritakagp-db".public.bakgrunnsjobb TO cloudsqliamuser;
GRANT ALL PRIVILEGES ON TABLE "fritakagp-db".public.flyway_schema_history TO cloudsqliamuser;
GRANT ALL PRIVILEGES ON TABLE "fritakagp-db".public.kvittering TO cloudsqliamuser;
alter default privileges in schema public grant all on tables to cloudsqliamuser;