/**
 * Geohash cells and distances, computed on the device.
 *
 * The app never sends its position to the server: it asks for the geohash cells around it
 * (apps/server geo/Geohash.java decodes them) and computes distances locally.
 * See docs/architecture.md, section 4.4.
 */

export type Coordinates = {
  latitude: number;
  longitude: number;
};

const BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz';
const EARTH_RADIUS_KM = 6371;

/** Standard geohash encoding (https://en.wikipedia.org/wiki/Geohash), same algorithm as the server. */
export function encodeGeohash({ latitude, longitude }: Coordinates, precision: number): string {
  let minLat = -90;
  let maxLat = 90;
  let minLng = -180;
  let maxLng = 180;
  let hash = '';
  let value = 0;
  let bits = 0;
  // Bits alternate between longitude and latitude, starting with longitude.
  let longitudeBit = true;

  while (hash.length < precision) {
    if (longitudeBit) {
      const mid = (minLng + maxLng) / 2;
      if (longitude >= mid) {
        value = value * 2 + 1;
        minLng = mid;
      } else {
        value *= 2;
        maxLng = mid;
      }
    } else {
      const mid = (minLat + maxLat) / 2;
      if (latitude >= mid) {
        value = value * 2 + 1;
        minLat = mid;
      } else {
        value *= 2;
        maxLat = mid;
      }
    }
    longitudeBit = !longitudeBit;
    bits++;
    if (bits === 5) {
      hash += BASE32[value];
      value = 0;
      bits = 0;
    }
  }
  return hash;
}

/**
 * The cell containing `point` and its 8 neighbours. Asking for the whole block keeps the
 * request the same wherever the user is inside the center cell.
 */
export function geohashBlock(point: Coordinates, precision: number): string[] {
  const totalBits = precision * 5;
  const cellWidth = 360 / 2 ** Math.ceil(totalBits / 2);
  const cellHeight = 180 / 2 ** Math.floor(totalBits / 2);
  const cells = new Set<string>();

  for (const dy of [-1, 0, 1]) {
    for (const dx of [-1, 0, 1]) {
      const latitude = Math.max(-90, Math.min(90, point.latitude + dy * cellHeight));
      const longitude = wrapLongitude(point.longitude + dx * cellWidth);
      cells.add(encodeGeohash({ latitude, longitude }, precision));
    }
  }
  return [...cells];
}

/** Great-circle distance (haversine formula), in kilometres. */
export function distanceKm(a: Coordinates, b: Coordinates): number {
  const toRadians = (degrees: number) => (degrees * Math.PI) / 180;
  const dLat = toRadians(b.latitude - a.latitude);
  const dLng = toRadians(b.longitude - a.longitude);
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRadians(a.latitude)) * Math.cos(toRadians(b.latitude)) * Math.sin(dLng / 2) ** 2;
  return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
}

function wrapLongitude(longitude: number): number {
  return ((((longitude + 180) % 360) + 360) % 360) - 180;
}
