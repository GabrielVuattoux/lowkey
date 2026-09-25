# Lowkey mobile

The Lowkey app for iOS and Android: [Expo](https://expo.dev) (React Native, TypeScript)
with [Expo Router](https://docs.expo.dev/router/introduction/). The design is described in
[docs/architecture.md](../../docs/architecture.md), and the plan in
[docs/roadmap.md](../../docs/roadmap.md).

## Run it

```bash
cd apps/mobile
npm install
npx expo start
```

Then press `i` for the iOS simulator (needs Xcode) or `a` for the Android emulator (needs
Android Studio), or scan the QR code with **Expo Go** on your phone.

Expo Go is enough while the app only uses modules bundled in Expo Go. When native modules
arrive (libsodium, MapLibre, SQLCipher), the app needs a
[development build](https://docs.expo.dev/develop/development-builds/introduction/):
`npx expo run:ios`, `npx expo run:android`, or `eas build --profile development`.

## Try it with the local server

1. Start the server with demo events around Zurich:
   `cd apps/server && ./gradlew bootRun --args='--spring.profiles.active=dev'`
2. Start the app with `npx expo start` and open it in Expo Go, on a phone connected to the
   same Wi-Fi as the computer.

The **Map** tab lists the public events around you. The app asks the server for the 3x3
block of geohash cells (about 5 km each) around your position, never for the position
itself, and computes distances on the phone.

In development, the app reaches the server on port 8080 of the computer that runs Metro.
To use another server, create `apps/mobile/.env.local` (ignored by git) with
`EXPO_PUBLIC_API_URL=http://<host>:8080`. Not in Zurich? Development builds show a
**Use Zurich (dev only)** button.

## Checks

```bash
npm test
npm run typecheck
npm run lint
```

## Layout

| Path | What |
|---|---|
| `src/app/` | Screens. Every file is a route; `_layout.tsx` defines the tab bar |
| `src/components/` | Shared components. `app-tabs.tsx` is native, `app-tabs.web.tsx` is web |
| `src/hooks/` | React hooks, e.g. `use-nearby-events.ts` (position → cells → public events) |
| `src/lib/` | Plain TypeScript: geohash and distances, API client, formatting. Unit tests in `__tests__/` |
| `src/constants/theme.ts` | Colors, spacing, fonts |
| `app.json` | App name, bundle identifier, deep-link scheme (`lowkey://`), permission texts |

Add dependencies with `npx expo install <package>` so versions match the Expo SDK.

The starter code comes from the Expo default template (MIT, see
[LICENSE-expo-template](LICENSE-expo-template)).
