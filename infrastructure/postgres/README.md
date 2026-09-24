# PostgreSQL infrastructure

Local service-owned PostgreSQL instances are declared in the root `docker-compose.yml`. Schema migrations remain inside the owning service; this directory is reserved for deployment-level PostgreSQL assets only.
