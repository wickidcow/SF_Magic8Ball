package com.xmooncorp.magic8ball.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class LocationUtilsTest {
    private static final double EPSILON = 1.0e-12;

    @ParameterizedTest
    @CsvSource({"0,-1,0", "90,0,-1", "180,1,0", "270,0,1", "360,-1,0", "-90,0,1"})
    void sidesFollowBukkitCardinalYaw(float yaw, double rightX, double rightZ) {
        Location origin = new Location(null, 0, 64, 0, yaw, 23);
        assertOffset(origin, LocationUtils.getRightSide(origin, 1), rightX, rightZ);
        assertOffset(origin, LocationUtils.getLeftSide(origin, 1), -rightX, -rightZ);
    }

    @ParameterizedTest
    @ValueSource(floats = {0, 15, 45, 90, 180, 270, 360, -90, 720})
    void sidesStayPerpendicularToFrontAndOppositeEachOther(float yaw) {
        Location origin = new Location(null, 12.5, 64, -7.25, yaw, -30);
        Vector front = offset(origin, LocationUtils.getFrontSide(origin, 1));
        Vector back = offset(origin, LocationUtils.getBackSide(origin, 1));
        Vector right = offset(origin, LocationUtils.getRightSide(origin, 1));
        Vector left = offset(origin, LocationUtils.getLeftSide(origin, 1));
        assertEquals(0, front.dot(right), EPSILON);
        assertEquals(0, front.dot(left), EPSILON);
        assertEquals(1, right.length(), EPSILON);
        assertEquals(1, left.length(), EPSILON);
        assertEquals(0, right.clone().add(left).length(), EPSILON);
        assertEquals(0, front.clone().add(back).length(), EPSILON);
    }

    @Test
    void requestedParticleOffsetIsPreserved() {
        Location origin = new Location(null, 0, 64, 0, 90, 0);
        assertOffset(origin, LocationUtils.getRightSide(origin, 0.325), 0, -0.325);
        assertOffset(origin, LocationUtils.getLeftSide(origin, 0.325), 0, 0.325);
    }

    @Test
    void helpersDoNotMutateOriginOrItsOrientation() {
        Location origin = new Location(null, 12.5, 64, -7.25, 45, 23);
        Location snapshot = origin.clone();
        Location[] results = {
            LocationUtils.getFrontSide(origin, 0.6),
            LocationUtils.getBackSide(origin, 0.6),
            LocationUtils.getRightSide(origin, 0.325),
            LocationUtils.getLeftSide(origin, 0.325)
        };
        assertEquals(snapshot, origin);
        for (Location result : results) {
            assertNotSame(origin, result);
            assertSame(origin.getWorld(), result.getWorld());
            assertEquals(origin.getY(), result.getY());
            assertEquals(origin.getYaw(), result.getYaw());
            assertEquals(origin.getPitch(), result.getPitch());
        }
    }

    @Test
    void zeroAndNegativeDistancesKeepTheirExistingMeaning() {
        Location origin = new Location(null, 12.5, 64, -7.25, 45, 0);
        assertEquals(origin, LocationUtils.getRightSide(origin, 0));
        assertEquals(origin, LocationUtils.getLeftSide(origin, 0));
        assertEquals(LocationUtils.getLeftSide(origin, 2), LocationUtils.getRightSide(origin, -2));
        assertEquals(LocationUtils.getRightSide(origin, 2), LocationUtils.getLeftSide(origin, -2));
    }

    private static Vector offset(Location origin, Location result) {
        return result.toVector().subtract(origin.toVector());
    }

    private static void assertOffset(Location origin, Location result, double x, double z) {
        Vector actual = offset(origin, result);
        assertEquals(x, actual.getX(), EPSILON);
        assertEquals(0, actual.getY(), EPSILON);
        assertEquals(z, actual.getZ(), EPSILON);
    }
}
