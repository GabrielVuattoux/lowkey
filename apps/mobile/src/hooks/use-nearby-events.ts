import * as Location from 'expo-location';
import { useCallback, useEffect, useState } from 'react';

import { fetchPublicEvents, type PublicEvent } from '@/lib/api';
import { distanceKm, geohashBlock, type Coordinates } from '@/lib/geo';

// Precision 5 cells are about 4.9 x 4.9 km, so the 3x3 block covers about 15 x 15 km.
// The server refuses anything finer.
const CELL_PRECISION = 5;

export type NearbyEvent = PublicEvent & { distanceKm: number };

export type NearbyEventsState =
  | { status: 'loading' }
  | { status: 'no-permission' }
  | { status: 'error'; message: string }
  | { status: 'ready'; events: NearbyEvent[] };

async function currentPosition(): Promise<Coordinates | null> {
  const permission = await Location.requestForegroundPermissionsAsync();
  if (permission.status !== Location.PermissionStatus.GRANTED) {
    return null;
  }
  const { coords } = await Location.getCurrentPositionAsync({
    accuracy: Location.Accuracy.Balanced,
  });
  return { latitude: coords.latitude, longitude: coords.longitude };
}

async function loadNearbyEvents(positionOverride?: Coordinates): Promise<NearbyEventsState> {
  try {
    const position = positionOverride ?? (await currentPosition());
    if (!position) {
      return { status: 'no-permission' };
    }
    const events = await fetchPublicEvents(geohashBlock(position, CELL_PRECISION));
    return {
      status: 'ready',
      events: events.map((event) => ({ ...event, distanceKm: distanceKm(position, event) })),
    };
  } catch (error) {
    return { status: 'error', message: error instanceof Error ? error.message : String(error) };
  }
}

/**
 * Public events around the user, soonest first. The position stays on the device: only the
 * geohash cells around it go to the server, and distances are computed here.
 */
export function useNearbyEvents() {
  const [positionOverride, setPositionOverride] = useState<Coordinates>();
  const [state, setState] = useState<NearbyEventsState>({ status: 'loading' });
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    let cancelled = false;
    loadNearbyEvents(positionOverride).then((next) => {
      if (!cancelled) {
        setState(next);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [positionOverride]);

  const refresh = useCallback(async () => {
    setRefreshing(true);
    setState(await loadNearbyEvents(positionOverride));
    setRefreshing(false);
  }, [positionOverride]);

  /** Development only: use this position instead of the device's (see the Map screen). */
  const overridePosition = useCallback((position: Coordinates) => {
    setState({ status: 'loading' });
    setPositionOverride(position);
  }, []);

  return { state, refreshing, refresh, positionOverride, overridePosition };
}
