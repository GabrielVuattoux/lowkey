import Constants from 'expo-constants';

import type { Coordinates } from '@/lib/geo';

export type EventCategory =
  | 'SPORTS'
  | 'MUSIC'
  | 'FOOD'
  | 'ART'
  | 'SOCIAL'
  | 'OUTDOOR'
  | 'PARTY'
  | 'OTHER';

/** What the server shares about a public event (PublicEventGetDTO). */
export type PublicEvent = Coordinates & {
  id: number;
  title: string;
  description: string;
  category: EventCategory;
  /** Server local time, ISO 8601 without offset. */
  startTime: string;
  endTime: string;
  participantCount: number;
};

/**
 * Base URL of the Lowkey server. EXPO_PUBLIC_API_URL overrides it. In development it defaults
 * to port 8080 on the machine running Metro, so a phone on the same Wi-Fi reaches the server
 * started with `./gradlew bootRun`.
 */
export const API_URL = resolveApiUrl();

const TIMEOUT_MS = 10_000;

function resolveApiUrl(): string {
  const fromEnv = process.env.EXPO_PUBLIC_API_URL;
  if (fromEnv) {
    return fromEnv.replace(/\/+$/, '');
  }
  const devHost = Constants.expoConfig?.hostUri?.split(':')[0];
  return `http://${devHost ?? 'localhost'}:8080`;
}

/** Public events in the given geohash cells. Only the cells leave the phone. */
export async function fetchPublicEvents(cells: string[]): Promise<PublicEvent[]> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const response = await fetch(`${API_URL}/public/events?cells=${cells.join(',')}`, {
      signal: controller.signal,
    });
    if (!response.ok) {
      throw new Error(`The server answered ${response.status}`);
    }
    return await response.json();
  } finally {
    clearTimeout(timeout);
  }
}
