# Lowkey

**Spontaneous events with your friends, without giving away your life.**

Lowkey is a social app for young people built around one idea: meeting up with
friends should not require handing your contacts, location and conversations to
a platform. It is inspired by privacy-first messengers such as Threema.

> Status: early stage. The code is currently the Spontaneo web app (map of
> spontaneous events, per-event chat and board). It is being reworked into a
> privacy-first mobile app for iOS and Android, with the web client kept alongside.

## Principles

- **Anonymous identity**: no phone number or email required.
- **Local-first**: user data lives on the device; the server stores as little as possible.
- **End-to-end encryption**: event chats and posts are unreadable by the server.
- **Coarse location**: the server never needs a user's exact position.
- **No ads, no tracking.**

## Repository layout

| Path | What |
|---|---|
| [`apps/client`](apps/client) | Web client (Next.js / TypeScript) |
| [`apps/server`](apps/server) | Backend (Spring Boot / Java 17) |
| [`docs/`](docs) | Architecture, roadmap, screenshots |

See each app's README for setup instructions.

## Design

- [Architecture](docs/architecture.md): threat model, identity, encryption, location, what the server stores.
- [Roadmap](docs/roadmap.md): phases and their branches.

## License and origin

Apache License 2.0, see [LICENSE](LICENSE). Lowkey is derived from *Spontaneo*,
built by SoPra FS26 Group 21 at the University of Zurich; see [NOTICE](NOTICE)
for attribution.
