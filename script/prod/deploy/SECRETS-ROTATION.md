# Secrets Rotation Runbook

This runbook is tailored for the current `bickdemo` deployment model:

- single backend service deployed by Docker Compose
- Jenkins/脚本保持现有命令不变
- real secrets live in `.env`, `BICKDEMO_ENV_FILE`, `/opt/bickdemo/.env`, or `$HOME/.bickdemo.env`

The goal is to rotate credentials without breaking deployment and without forcing avoidable logouts.

## Before You Start

1. Put the next set of secrets in a new external env file.
2. Do not overwrite the old file first. Keep both old and new values available until validation finishes.
3. Rotate one subsystem at a time and verify before moving on.

Suggested naming:

- current file: `/opt/bickdemo/.env`
- staged file: `/opt/bickdemo/.env.next`
- optional explicit pointer: `BICKDEMO_ENV_FILE=/opt/bickdemo/.env`

## 1. JWT Secret

Current code now supports:

- `JWT_SECRET`: signs all new tokens
- `JWT_PREVIOUS_SECRETS`: comma-separated old Base64 secrets accepted only for validation

Safe rotation steps:

1. Generate a new Base64 JWT secret.
2. In the env file, set:
   - `JWT_SECRET=<new secret>`
   - `JWT_PREVIOUS_SECRETS=<old secret>`
3. Deploy normally.
4. Wait at least `jwt.expiration` time. The current config is 86400000 ms, which is 24 hours.
5. Remove the old secret from `JWT_PREVIOUS_SECRETS` in the next deploy.

Effect:

- new logins get new tokens immediately
- already logged-in users keep working until their old token expires

## 2. MySQL

Do not rotate by changing `root` directly first. Migrate the app to a dedicated user.

Recommended sequence:

1. Create a new app user, for example `bickdemo_app_next`.
2. Grant only required permissions on database `bickdemo`.
3. Update env:
   - `MYSQL_USERNAME=bickdemo_app_next`
   - `MYSQL_PASSWORD=<new password>`
4. Deploy normally.
5. Verify login, bicycle list, rentals, forum, admin pages.
6. Revoke the old app user after verification.

If you are still using `root`, do this in two phases:

1. create and switch to dedicated user
2. only then rotate or stop using `root`

## 3. RabbitMQ

RabbitMQ already supports a clean two-user handover.

Steps:

1. Create a new RabbitMQ user.
2. Grant permissions to the same vhost/exchanges/queues.
3. Update env:
   - `RABBITMQ_USERNAME=<new user>`
   - `RABBITMQ_PASSWORD=<new password>`
4. Deploy normally.
5. Verify friend requests, chat messages, read receipts.
6. Delete or disable the old RabbitMQ user.

## 4. Redis

Current config now supports:

- `REDIS_USERNAME`
- `REDIS_PASSWORD`

That means you can use Redis 6 ACL users instead of a single shared password.

Safe rotation steps:

1. Create a new Redis ACL user with the required command/key permissions.
2. Update env:
   - `REDIS_USERNAME=<new acl user>`
   - `REDIS_PASSWORD=<new password>`
3. Deploy normally.
4. Verify login verification codes, cache hits, blacklist/rate-limit features.
5. Disable the old Redis ACL user.

If your Redis only uses `requirepass` and not ACL users, rotation is inherently more disruptive. Prefer enabling ACL first.

## 5. MinIO

MinIO rotation works best with a second access key pair.

Steps:

1. Create a new MinIO service account or access key pair.
2. Confirm it can read/write the `bicycles` bucket.
3. Update env:
   - `MINIO_ACCESS_KEY=<new key>`
   - `MINIO_SECRET_KEY=<new secret>`
4. Deploy normally.
5. Verify upload, avatar update, forum image upload, background image upload.
6. Disable the old access key.

## 6. SMTP / Mail Auth Code

Steps:

1. Generate a new SMTP authorization code in the mail provider.
2. Update env:
   - `MAIL_USERNAME=<mail account>`
   - `MAIL_PASSWORD=<new auth code>`
3. Deploy normally.
4. Verify registration code, email login code, password reset.
5. Revoke the old authorization code.

## 7. Operational Pattern

Use this pattern for each credential family:

1. Create new credential alongside the old one.
2. Update only the app env file.
3. Deploy with the existing Jenkins job or deployment script.
4. Verify target functions.
5. Remove the old credential only after successful verification.

## 8. Rollback

If any subsystem fails after rotation:

1. restore the previous env file
2. redeploy with the same existing command
3. keep the new credential disabled but not deleted until root cause is clear

For JWT specifically:

- rollback can keep `JWT_SECRET=<old>`
- or keep `JWT_SECRET=<new>` and move the old one into `JWT_PREVIOUS_SECRETS`

## 9. Suggested Order

Recommended rotation order for this project:

1. JWT
2. SMTP
3. RabbitMQ
4. MinIO
5. Redis
6. MySQL

Reason:

- JWT and SMTP are easiest to validate and least coupled to storage
- MySQL is the highest-impact dependency and should be last
