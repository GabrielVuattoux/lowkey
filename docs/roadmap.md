# Lowkey: roadmap

Each phase has its own branch, created from `main`. Work happens on the branch and
reaches `main` through a pull request. Branches created ahead of time are placeholders:
before starting one, rebase it on the latest `main` (`git rebase main`).

See [architecture.md](architecture.md) for the reasoning behind each step.

| # | Branch | Scope | Needs |
|---|---|---|---|
| 0 | `main` | Monorepo bootstrap, NOTICE, architecture and roadmap | done |
| 1 | `feature/mobile-app` | Expo app skeleton for iOS and Android: tabs (Map, Events, Friends, Profile), app config, `lowkey://` deep-link scheme. "Around you" list of public events, served without an account by `GET /public/events?cells=…` (geohash cells, never coordinates) | 0 |
| 1 | `chore/monorepo-ci` | GitHub Actions with path filters: client build, server tests, mobile typecheck and tests | 0 |
| 1 | `fix/server-privacy-quick-wins` | Hash passwords (BCrypt) while password login still exists; stop exposing emails through `GET /users`; make join/leave act on the authenticated user, not on a `userId` taken from the request | 0 |
| 2 | `feature/anonymous-identity` | On-device key pairs, Lowkey ID, challenge-response login, friends via QR code or invite link; removes email and password accounts | 1 |
| 3 | `feature/e2e-messaging` | Encrypted local SQLite, mailbox relay on the server, libsodium envelopes, event group keys, encrypted photos | 2 |
| 4 | `feature/private-map` | MapLibre + self-hosted OSM tiles (mobile and web), public events queried by geohash cell, private events kept inside the encrypted group | 1 (3 for private events) |
| 5 | `feature/push-notifications` | Direct APNs / FCM, content-free wake-up pushes | 3 |
| 6 | `feature/encrypted-backup` | Passphrase backup (Argon2id), restore on a new phone, local export | 2 |
| 7 | `feature/safety-moderation` | Block, mute, reports with signed evidence, age gate | 3 |

## Milestones

- **M1 (usable skeleton):** phase 1. The mobile app runs on iOS and Android and shows public events from the existing server.
- **M2 (private by design):** phases 2 to 4. Anonymous accounts, encrypted chat, no position leaks. The first version worth showing to friends.
- **M3 (ready for testers):** phases 5 to 7. Notifications, backup and safety tools. The minimum before a TestFlight or closed Android beta.
