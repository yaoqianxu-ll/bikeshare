# script

This directory is organized by environment and follows a deployment-oriented layout.

- `dev/`: local development and testing
- `prod/`: production deployment

Recommended usage:

- keep environment variables in `.env` based on `env.example`
- start services through `start.sh` or `start.bat`
- keep deployment helpers under `prod/deploy/`
- keep HTTPS related files under `prod/ssl/`

The original runtime sources still live in:

- `bickdemo-backend/src/main/resources/`
- `script/prod/docker-compose.yml`
- `bickdemo-frontend/nginx.conf`
