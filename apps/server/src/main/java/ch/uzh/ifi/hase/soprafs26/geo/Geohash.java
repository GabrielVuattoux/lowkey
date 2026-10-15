package ch.uzh.ifi.hase.soprafs26.geo;

import java.util.Locale;

/**
 * Minimal geohash decoding (https://en.wikipedia.org/wiki/Geohash).
 *
 * Lowkey clients ask for public events by geohash cell instead of sending their position,
 * so the server only needs to turn a cell into its bounding box. The mobile app encodes the
 * cells (apps/mobile/src/lib/geo.ts); both sides must agree on the standard algorithm.
 */
public final class Geohash {

    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    public record BoundingBox(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
    }

    private Geohash() {
    }

    public static boolean isValid(String geohash) {
        if (geohash == null || geohash.isEmpty()) {
            return false;
        }
        for (char c : geohash.toLowerCase(Locale.ROOT).toCharArray()) {
            if (BASE32.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }

    public static BoundingBox decode(String geohash) {
        if (!isValid(geohash)) {
            throw new IllegalArgumentException("Invalid geohash");
        }
        double minLat = -90;
        double maxLat = 90;
        double minLng = -180;
        double maxLng = 180;
        // Bits alternate between longitude and latitude, starting with longitude.
        boolean longitudeBit = true;
        for (char c : geohash.toLowerCase(Locale.ROOT).toCharArray()) {
            int value = BASE32.indexOf(c);
            for (int bit = 4; bit >= 0; bit--) {
                boolean set = ((value >> bit) & 1) == 1;
                if (longitudeBit) {
                    double mid = (minLng + maxLng) / 2;
                    if (set) {
                        minLng = mid;
                    } else {
                        maxLng = mid;
                    }
                } else {
                    double mid = (minLat + maxLat) / 2;
                    if (set) {
                        minLat = mid;
                    } else {
                        maxLat = mid;
                    }
                }
                longitudeBit = !longitudeBit;
            }
        }
        return new BoundingBox(minLat, maxLat, minLng, maxLng);
    }
}
