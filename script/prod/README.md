# prod

Production deployment files.

Structure:

- `application-prod.yml`: production backend configuration snapshot
- root `docker-compose.yml`: current production compose primary entry
- `docker-compose.yml`: mirror copy of the root compose file, kept for config organization
- `env.example`: production environment variable example
- `nginx.conf`: frontend reverse proxy config
- `start.sh` / `start.bat`: wrappers around the root compose startup
- `deploy/`: deployment helper scripts
