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

## Checks

```bash
npm run typecheck
npm run lint
```

## Layout

| Path | What |
|---|---|
| `src/app/` | Screens. Every file is a route; `_layout.tsx` defines the tab bar |
| `src/components/` | Shared components. `app-tabs.tsx` is native, `app-tabs.web.tsx` is web |
| `src/constants/theme.ts` | Colors, spacing, fonts |
| `app.json` | App name, bundle identifier, deep-link scheme (`lowkey://`) |

Add dependencies with `npx expo install <package>` so versions match the Expo SDK.

The starter code comes from the Expo default template (MIT, see
[LICENSE-expo-template](LICENSE-expo-template)).
