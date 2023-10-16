package me.mykindos.betterpvp.core.utilities;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class UtilMath {

    public static Random random = new Random(System.nanoTime());

    public static Transformation rotateAround(Transformation transformation, Quaternionf left, Quaternionf right, Vector3f relativePivot) {
        // Apply rotations
        Quaternionf rotation = new Quaternionf();
        left.mul(right, rotation);

        Matrix4f transformationMatrix = new Matrix4f();
        transformationMatrix.translate(relativePivot);
        transformationMatrix.rotate(rotation);
        transformationMatrix.translate(relativePivot.negate());
        final com.mojang.math.Transformation result = new com.mojang.math.Transformation(transformationMatrix);

        return new Transformation(
                transformation.getTranslation().add(result.getTranslation()),
                result.getLeftRotation(),
                result.getScale(),
                result.getRightRotation()
        );
    }


    /**
     * Generates a random integer with a max value
     *
     * @param value Max value the random int can be
     * @return Returns a random integer between 0 and the value provided
     */
    public static int randomInt(int value) {

        return ThreadLocalRandom.current().nextInt(value);
    }

    /**
     * Generates a random integer within a range
     *
     * @param min Minimum value
     * @param max Maximum value
     * @return Returns a random integer between the min and max provided
     */
    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    /**
     * Generates a random double within a range
     *
     * @param min Minimum value
     * @param max Maximum value
     * @return Returns a random double between the min and max provided
     */
    public static double randDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * Calculates the angle between 2 vectors
     *
     * @param vec1 Vector 1
     * @param vec2 Vector 2
     * @return Returns the angle between 2 vectors as a double
     */
    public static double getAngle(Vector vec1, Vector vec2) {
        return vec1.angle(vec2) * 180.0F / 3.141592653589793D;
    }


    /**
     * Calculates the offset between 2 locations
     *
     * @param a Location 1
     * @param b Location 2
     * @return Returns the offset between 2 locations
     */
    public static double offset(Location a, Location b) {
        return offset(a.toVector(), b.toVector());
    }

    /**
     * Calculates the offset between 2 vectors
     *
     * @param a Vector 1
     * @param b Vector 2
     * @return Returns the offset between 2 vectors
     */
    public static double offset(Vector a, Vector b) {
        return a.subtract(b).length();
    }

    /**
     * Calculates the offset between 2 entities
     *
     * @param a Entity 1
     * @param b Entity 2
     * @return Returns the offset between 2 entities
     */
    public static double offset(Entity a, Entity b) {
        return offset(a.getLocation().toVector(), b.getLocation().toVector());
    }

    /**
     * Calculates the 2d offset between 2 locations
     *
     * @param a Location 1
     * @param b Location 2
     * @return Returns the 2d offset between 2 locations
     */
    public static double offset2d(Location a, Location b) {
        return offset2d(a.toVector(), b.toVector());
    }

    /**
     * Calculates the 2d offset between 2 vectors
     *
     * @param a Vector 1
     * @param b Vector 2
     * @return Returns the 2d offset between 2 vectors
     */
    public static double offset2d(Vector a, Vector b) {
        a.setY(0);
        b.setY(0);
        return a.subtract(b).length();
    }

    /**
     * Gets an Integer from a string value and adjusts
     * the value to be within the defined range if necessary
     *
     * @param value String to get Integer from
     * @param min   Min value
     * @param max   Max value
     * @return Returns an integer from a String, while enforcing the min / max values
     */
    public static int getInteger(String value, int min, int max) {
        int i = min;
        try {
            i = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }

        if (i < min) {
            i = min;
        } else if (i > max) {
            i = max;
        }
        return i;
    }


    /**
     * Returns a set of Locations without the inner blocks
     * E.g. Creates a hollow sphere or box
     *
     * @param blocks Blocks to calculate
     * @param sphere Whether or not you are trying to hollow out a sphere
     * @return
     */
    public static Set<Location> makeHollow(Set<Location> blocks, boolean sphere) {
        Set<Location> edge = new HashSet<Location>();
        if (!sphere) {
            for (Location l : blocks) {
                World w = l.getWorld();
                int X = l.getBlockX();
                int Y = l.getBlockY();
                int Z = l.getBlockZ();
                Location front = new Location(w, X + 1, Y, Z);
                Location back = new Location(w, X - 1, Y, Z);
                Location left = new Location(w, X, Y, Z + 1);
                Location right = new Location(w, X, Y, Z - 1);
                if (!(blocks.contains(front) && blocks.contains(back) && blocks.contains(left) && blocks.contains(right))) {
                    edge.add(l);
                }
            }
            return edge;
        } else {
            for (Location l : blocks) {
                World w = l.getWorld();
                int X = l.getBlockX();
                int Y = l.getBlockY();
                int Z = l.getBlockZ();
                Location front = new Location(w, X + 1, Y, Z);
                Location back = new Location(w, X - 1, Y, Z);
                Location left = new Location(w, X, Y, Z + 1);
                Location right = new Location(w, X, Y, Z - 1);
                Location top = new Location(w, X, Y + 1, Z);
                Location bottom = new Location(w, X, Y - 1, Z);
                if (!(blocks.contains(front) && blocks.contains(back) && blocks.contains(left) && blocks.contains(right) && blocks.contains(top) && blocks.contains(bottom))) {
                    edge.add(l);
                }
            }
            return edge;
        }
    }

    /**
     * Creates a set of Locations to create a sphere of any size and hollow
     *
     * @param location Center of the sphere location
     * @param radius   The radius of the Sphere
     * @param hollow   Whether or not the sphere should be hollow
     * @return Returns a set of locations to create a sphere
     */
    public static Set<Location> sphere(Location location, int radius, boolean hollow) {
        Set<Location> blocks = new HashSet<Location>();
        World world = location.getWorld();
        int X = location.getBlockX();
        int Y = location.getBlockY();
        int Z = location.getBlockZ();
        int radiusSquared = radius * radius;

        if (hollow) {
            for (int x = X - radius; x <= X + radius; x++) {
                for (int y = Y - radius; y <= Y + radius; y++) {
                    for (int z = Z - radius; z <= Z + radius; z++) {
                        if ((X - x) * (X - x) + (Y - y) * (Y - y) + (Z - z) * (Z - z) <= radiusSquared) {
                            Location block = new Location(world, x, y, z);
                            blocks.add(block);
                        }
                    }
                }
            }
            return makeHollow(blocks, true);
        } else {
            for (int x = X - radius; x <= X + radius; x++) {
                for (int y = Y - radius; y <= Y + radius; y++) {
                    for (int z = Z - radius; z <= Z + radius; z++) {
                        if ((X - x) * (X - x) + (Y - y) * (Y - y) + (Z - z) * (Z - z) <= radiusSquared) {
                            Location block = new Location(world, x, y, z);
                            blocks.add(block);
                        }
                    }
                }
            }
            return blocks;
        }
    }

    /**
     * Round a double to specified decimal places
     * @param value Value to round
     * @param places Decimal places
     * @return rounded double
     */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

}
