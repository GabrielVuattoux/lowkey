package ch.uzh.ifi.hase.soprafs26.geo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GeohashTest {

    private static void assertContains(Geohash.BoundingBox box, double latitude, double longitude) {
        assertTrue(latitude >= box.minLatitude() && latitude <= box.maxLatitude(), "latitude outside the cell");
        assertTrue(longitude >= box.minLongitude() && longitude <= box.maxLongitude(), "longitude outside the cell");
    }

    @Test
    public void decode_wikipediaExample_returnsExpectedBox() {
        // https://en.wikipedia.org/wiki/Geohash: "ezs42" is about 42.605, -5.603
        Geohash.BoundingBox box = Geohash.decode("ezs42");

        assertEquals(42.5830078125, box.minLatitude(), 1e-9);
        assertEquals(42.626953125, box.maxLatitude(), 1e-9);
        assertEquals(-5.625, box.minLongitude(), 1e-9);
        assertEquals(-5.5810546875, box.maxLongitude(), 1e-9);
    }

    @Test
    public void decode_fullPrecisionHash_containsOriginalPoint() {
        // https://en.wikipedia.org/wiki/Geohash: 57.64911, 10.40744 encodes to "u4pruydqqvj"
        assertContains(Geohash.decode("u4pruydqqvj"), 57.64911, 10.40744);
    }

    @Test
    public void decode_zurichCell_containsZurichMainStation() {
        // Same value as the mobile app computes for Zurich HB at precision 5
        assertContains(Geohash.decode("u0qjd"), 47.3769, 8.5417);
    }

    @Test
    public void decode_isCaseInsensitive() {
        assertEquals(Geohash.decode("u0qjd"), Geohash.decode("U0QJD"));
    }

    @Test
    public void isValid_rejectsCharactersOutsideTheAlphabet() {
        // a, i, l and o are not part of the geohash base32 alphabet
        assertFalse(Geohash.isValid("u0qja"));
        assertFalse(Geohash.isValid("u0-jd"));
        assertFalse(Geohash.isValid(""));
        assertFalse(Geohash.isValid(null));
        assertTrue(Geohash.isValid("u0qjd"));
    }

    @Test
    public void decode_invalidHash_throws() {
        assertThrows(IllegalArgumentException.class, () -> Geohash.decode("hello"));
    }
}
