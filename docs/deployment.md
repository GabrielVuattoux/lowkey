# Lowkey: deployment

The server ships as a Docker image ([`apps/server/Dockerfile`](../apps/server/Dockerfile)) that
runs the `prod` profile on any PostgreSQL database. Nothing tied to a hosting provider lives in
the repository: moving to another host means running the same image against another database.

## Configuration

The image reads its configuration from environment variables
([`application-prod.properties`](../apps/server/src/main/resources/application-prod.properties)):

| Variable | Required | Example | Purpose |
|---|---|---|---|
| `DB_URL` | yes | `jdbc:postgresql://db.example.com:25060/lowkey?sslmode=require` | JDBC URL of the database |
| `DB_USERNAME` | yes | `lowkey` | Database user |
| `DB_PASSWORD` | yes | (secret) | Database password. Store it as a secret on the platform, never in the repo |
| `DB_POOL_SIZE` | no, default 5 | `5` | Maximum database connections |
| `PORT` | no, default 8080 | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | set to `prod` by the image | `prod,dev` | `dev` adds the demo events around Zurich |

If a `DB_*` variable is missing, the server refuses to start and names the missing variable.

## Run it locally like in production

```bash
cd apps/server
docker compose up --build      # PostgreSQL + the server image, on http://localhost:8080
docker compose down -v         # stop and delete the local database
```

[`compose.yaml`](../apps/server/compose.yaml) starts the image with the `prod` and `dev` profiles,
so the demo events are there. The phone setup is the same as with `./gradlew bootRun`.

## DigitalOcean App Platform (planned)

Prerequisite: the DigitalOcean credits of the GitHub Student Developer Pack.

1. **Create an app** from GitHub: repository `GabrielVuattoux/lowkey`, branch `main`, source directory
   `apps/server`. App Platform detects the Dockerfile and redeploys on every push to `main`.
2. **Add a PostgreSQL database** and attach it to the app (a dev database to start, a managed
   cluster later). The steps below assume the database component is called `db`.
3. **Set the server's environment variables**, using App Platform's bindable variables:
   - `DB_URL` = `jdbc:postgresql://${db.HOSTNAME}:${db.PORT}/${db.DATABASE}?sslmode=require`
   - `DB_USERNAME` = `${db.USERNAME}`
   - `DB_PASSWORD` = `${db.PASSWORD}`, marked as encrypted

   Check the resolved values in the app's Console tab: the exact variable names come from
   DigitalOcean's documentation and are not tested here yet.
4. **HTTP port** `8080`, **health check** path `/ping`.
5. **Region**: Frankfurt (FRA1) or Amsterdam (AMS3), the closest to Switzerland.

The app gets an HTTPS address such as `https://lowkey-xxxxx.ondigitalocean.app`. Point the
clients to it:

- **Mobile**: `EXPO_PUBLIC_API_URL=https://…` (in `apps/mobile/.env.local`, or in the EAS environment
  for published builds)
- **Web client**: `NEXT_PUBLIC_API_URL=https://…`, set at build time (Next.js inlines it)

## Moving to another host later (Hetzner, Infomaniak, Exoscale…)

Run the same image next to any PostgreSQL, behind a reverse proxy that provides HTTPS (for
example Caddy):

```bash
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://<host>:5432/lowkey -e DB_USERNAME=lowkey -e DB_PASSWORD=<secret> \
  <image>
```

Back up the database before moving (`pg_dump`) and restore it on the new host (`pg_restore`).

## Before real users

- **Database migrations:** the schema is updated by Hibernate (`ddl-auto=update`), which is
  fine for tests. Switch to versioned migrations (Flyway) before data matters.
- **CORS:** `Application.java` allows every origin. That is fine for the mobile app, but restrict it
  to the web client's domain once that is public.
- **Backups:** turn on the database's automatic backups.
- **Time zones:** the server stores local times without an offset and runs in the
  `Europe/Zurich` zone (set in the Dockerfile). Moving the API to UTC timestamps with an offset
  is needed before users outside Switzerland.
