# Agent Notes (bickdemo)

This repository is a small monorepo for a BikeShare / bicycle rental demo:

- `bickdemo-backend/`: Spring Boot 3 (Java 17, Maven), MyBatis-Plus, Spring Security + JWT, MySQL/H2, MinIO, Caffeine.
- `bickdemo-frontend/`: Vue 3 + Vite + Element Plus + Pinia + Axios + ECharts.
- `script/prod/docker-compose.yml`: MySQL + backend + frontend deployment.

## Quick Start (Local Dev)

Backend (default port `8080`):

- Config: `bickdemo-backend/src/main/resources/application.yml`
- Typical commands:
  - `cd bickdemo-backend`
  - `mvn -q -DskipTests=false test`
  - `mvn spring-boot:run`

Frontend (default dev port `5173`):

- Proxy: `bickdemo-frontend/vite.config.js` proxies `/api` -> `http://localhost:8080`
- Typical commands:
  - `cd bickdemo-frontend`
  - `npm install`
  - `npm run dev`

Database:

- SQL bootstrap file: `sql/init.sql` and `bickdemo-backend/init-db/` (Docker init folder)
- Local `application.yml` points to MySQL `bickdemo` on `localhost:3306`.

## Docker

`script/prod/docker-compose.yml` exposes:

- MySQL: `3306`
- Backend: `8080` (profile `prod`)
- Frontend (nginx): `80`

Typical commands:

- `docker compose -f script/prod/docker-compose.yml up -d --build`
- `docker compose -f script/prod/docker-compose.yml logs -f --tail=100`
- `docker compose -f script/prod/docker-compose.yml down`

Note: `bickdemo-frontend/nginx.conf` proxies `/api` to `http://bickdemo-app:8080`. Ensure the upstream name matches the backend service/container name defined in the compose setup you use (root compose uses service `app`).

## API Conventions

Frontend Axios wrapper in `bickdemo-frontend/src/api/request.js` expects a unified response shape:

- Success: `res.code === 200`
- Errors: shows `res.message`, and for `400` may render field validation details from `data.data`.

If changing backend responses, keep this contract in sync with the frontend.

Base routes (from controllers):

- `/api/auth`
- `/api/bicycles`
- `/api/rentals`
- `/api/statistics`
- `/api/files`
- `/api/backgrounds`

## Security / Secrets

This repo currently contains credentials/secrets in config and compose files. For real deployments:

- Move secrets to env vars (e.g. `.env`, CI credentials store) and keep them out of git history.
- Prefer `application-prod.yml` env overrides for prod-like runs.

## Tooling Notes (Windows / PowerShell)

- If `rg` (ripgrep) is blocked in your environment ("Access denied"), use PowerShell alternatives:
  - `Get-ChildItem -Recurse -File | Select-String -Pattern "..."` for search.
- If Chinese text looks garbled in terminal output, prefer explicit UTF-8 reads:
  - `Get-Content -Encoding UTF8 path/to/file`

## Database Migrations (Common)

When pulling newer commits into an existing MySQL volume, you may need to apply schema changes manually.

- Bicycle inventory: `ALTER TABLE bicycles ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '数量（库存）';`
- Rental quantity: `ALTER TABLE rentals ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '租赁数量';`

