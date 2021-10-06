[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=navikt_fritakagp)](https://sonarcloud.io/dashboard?id=navikt_fritakagp)


Fritak Arbeidsgiverperiode (AGP) 
================


Backend for mottak av søknader om fritak fra AGP ved sykepenger.
# Komme i gang

For å kjøre lokalt kan du starte  `docker-compose up` fra docker/local før du starter prosjektet. 

# Koble til Databasen i GCP

Følg oppskriften for Cloud SQL proxy her: https://doc.nais.io/persistence/postgres/

For å koble til når du har personlig bruker:
CONNECTION_NAME=$(gcloud sql instances describe fritakagp --format="get(connectionName)" --project helsearbeidsgiver-dev-6d06);
./cloud_sql_proxy -instances=${CONNECTION_NAME}=tcp:5555
gcloud auth print-access-token

Koble til localhost:5555 med nav epost og access tokenet som blir printa over 

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #helse-arbeidsgiver.
