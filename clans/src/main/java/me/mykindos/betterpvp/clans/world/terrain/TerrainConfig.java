package me.mykindos.betterpvp.clans.world.terrain;

import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.world.terrain.TerrainScanParameters;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Resolves terrain-scan settings per world, with a {@code default} profile as the fallback. This is what makes the scan
 * generic: {@code clans.terrain.<world>.<key>} overrides {@code clans.terrain.default.<key>}, so different worlds can
 * carry different sea levels, beach widths, mountain heights, and ocean damage without any per-world code.
 */
public final class TerrainConfig {

    private static final String BASE = "clans.terrain.";

    private TerrainConfig() {
    }

    public static @NotNull TerrainScanParameters scanParameters(@NotNull ExtendedYamlConfiguration config,
                                                                @NotNull String world) {
        return new TerrainScanParameters(
                getInt(config, world, "seaLevel", 62),
                getInt(config, world, "beachMinWidth", 15),
                getInt(config, world, "beachMaxWidth", 30),
                getInt(config, world, "mountainBaseY", 86),
                parseMaterials(getString(config, world, "beachMaterials", "SAND,RED_SAND,GRAVEL,SANDSTONE")));
    }

    public static int maxScanChunks(@NotNull ExtendedYamlConfiguration config, @NotNull String world) {
        return getInt(config, world, "maxScanChunks", 40000);
    }

    public static double oceanDamage(@NotNull ExtendedYamlConfiguration config, @NotNull String world) {
        return getDouble(config, world, "oceanDamage", 3.0);
    }

    public static int getInt(@NotNull ExtendedYamlConfiguration config, @NotNull String world, @NotNull String key, int def) {
        return config.getInt(BASE + world + "." + key, config.getInt(BASE + "default." + key, def));
    }

    public static double getDouble(@NotNull ExtendedYamlConfiguration config, @NotNull String world, @NotNull String key, double def) {
        return config.getDouble(BASE + world + "." + key, config.getDouble(BASE + "default." + key, def));
    }

    public static @NotNull String getString(@NotNull ExtendedYamlConfiguration config, @NotNull String world,
                                            @NotNull String key, @NotNull String def) {
        return config.getString(BASE + world + "." + key, config.getString(BASE + "default." + key, def));
    }

    public static @NotNull Set<Material> parseMaterials(@NotNull String raw) {
        final Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String token : raw.split(",")) {
            final Material material = Material.matchMaterial(token.trim());
            if (material != null) {
                materials.add(material);
            }
        }
        return materials;
    }
}
