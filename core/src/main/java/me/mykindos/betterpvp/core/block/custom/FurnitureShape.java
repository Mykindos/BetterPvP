package me.mykindos.betterpvp.core.block.custom;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * How a piece of furniture looks and what space it takes. The model is an item model drawn by an item display at the
 * centre of the base block. Barriers and lights are block offsets from the base block for furniture facing south, and
 * turn with the furniture.
 */
@Value
@Builder
public class FurnitureShape {

    Key model;
    @Builder.Default
    Vector3f translation = new Vector3f();
    @Builder.Default
    Vector3f scale = new Vector3f(1, 1, 1);
    @Singular
    List<Vector> barriers;
    @Singular
    Map<Vector, Integer> lights;
    /**
     * Break hardness, on the vanilla scale (stone is 1.5).
     */
    double hardness;
    /**
     * The block whose place, hit and break sounds the furniture makes.
     */
    @Builder.Default
    Material sounds = Material.STONE;

    /**
     * Block offsets from specs such as {@code "0..1,0..3,-1..0"}: each axis is a number or an inclusive range.
     */
    public static List<Vector> offsets(String... specs) {
        final List<Vector> offsets = new ArrayList<>();
        for (String spec : specs) {
            final String[] axes = spec.split(",");
            final int[][] ranges = new int[3][];
            for (int i = 0; i < 3; i++) {
                final String[] bounds = axes[i].trim().split("\\.\\.");
                final int from = Integer.parseInt(bounds[0]);
                ranges[i] = new int[]{from, bounds.length > 1 ? Integer.parseInt(bounds[1]) : from};
            }
            for (int x = ranges[0][0]; x <= ranges[0][1]; x++) {
                for (int y = ranges[1][0]; y <= ranges[1][1]; y++) {
                    for (int z = ranges[2][0]; z <= ranges[2][1]; z++) {
                        offsets.add(new Vector(x, y, z));
                    }
                }
            }
        }
        return offsets;
    }

    /**
     * An offset turned to a furniture yaw, which is always a multiple of 90 degrees. Offsets count z the other way
     * round from the world, so +z sits on the side the furniture faces away from.
     */
    public static Vector rotate(Vector offset, float yaw) {
        final int quarter = Math.floorMod(Math.round(yaw / 90f), 4);
        final int x = offset.getBlockX();
        final int z = -offset.getBlockZ();
        return switch (quarter) {
            case 1 -> new Vector(-z, offset.getBlockY(), x);
            case 2 -> new Vector(-x, offset.getBlockY(), -z);
            case 3 -> new Vector(z, offset.getBlockY(), -x);
            default -> new Vector(x, offset.getBlockY(), z);
        };
    }

}
