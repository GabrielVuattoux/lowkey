import { distanceKm, encodeGeohash, geohashBlock } from '@/lib/geo';

const ZURICH_HB = { latitude: 47.3769, longitude: 8.5417 };
const BERN = { latitude: 46.9481, longitude: 7.4474 };

describe('encodeGeohash', () => {
  it('matches the reference value from https://en.wikipedia.org/wiki/Geohash', () => {
    expect(encodeGeohash({ latitude: 57.64911, longitude: 10.40744 }, 11)).toBe('u4pruydqqvj');
  });

  it('matches the cells the server tests use', () => {
    // Same values as apps/server GeohashTest and PublicEventsIntegrationTest
    expect(encodeGeohash(ZURICH_HB, 5)).toBe('u0qjd');
    expect(encodeGeohash(BERN, 5)).toBe('u0m71');
    expect(encodeGeohash(ZURICH_HB, 4)).toBe('u0qj');
  });
});

describe('geohashBlock', () => {
  it('returns the cell of the point and its 8 neighbours', () => {
    const block = geohashBlock(ZURICH_HB, 5);

    expect(block).toHaveLength(9);
    expect(new Set(block).size).toBe(9);
    expect(block).toContain('u0qjd');
    expect(block.every((cell) => cell.length === 5)).toBe(true);
  });

  it('only depends on the center cell, not on the exact position inside it', () => {
    // Two points in the same precision-5 cell produce the same request
    const a = geohashBlock({ latitude: 47.3769, longitude: 8.5417 }, 5);
    const b = geohashBlock({ latitude: 47.3790, longitude: 8.5390 }, 5);

    expect(encodeGeohash({ latitude: 47.379, longitude: 8.539 }, 5)).toBe('u0qjd');
    expect([...a].sort()).toEqual([...b].sort());
  });

  it('wraps around the antimeridian', () => {
    const block = geohashBlock({ latitude: 0, longitude: 179.99 }, 4);

    expect(block).toHaveLength(9);
    // Neighbours east of 180° are encoded as longitudes near -180°
    expect(block.some((cell) => cell.startsWith('8'))).toBe(true);
  });
});

describe('distanceKm', () => {
  it('is zero for the same point', () => {
    expect(distanceKm(ZURICH_HB, ZURICH_HB)).toBe(0);
  });

  it('computes the Zurich to Bern distance', () => {
    // About 95 km as the crow flies
    expect(distanceKm(ZURICH_HB, BERN)).toBeGreaterThan(94);
    expect(distanceKm(ZURICH_HB, BERN)).toBeLessThan(96);
  });
});
