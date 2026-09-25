#!/usr/bin/env bash
# Lance Spring Boot en mode prod (Cloud SQL Postgres) en local.
# Charge DB_PASSWORD depuis .env.local (gitignoré).
# Prérequis: gcloud auth application-default login (une seule fois).

set -e

if [ ! -f .env.local ]; then
  echo "❌ .env.local introuvable. Crée-le avec: DB_PASSWORD=<password spring-app>" >&2
  exit 1
fi

set -a
. ./.env.local
set +a

export SPRING_PROFILES_ACTIVE=prod
exec ./gradlew bootRun
