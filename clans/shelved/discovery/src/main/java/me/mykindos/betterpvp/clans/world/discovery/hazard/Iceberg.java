package me.mykindos.betterpvp.clans.world.discovery.hazard;

import com.google.inject.Provider;
import lombok.Value;
import me.mykindos.betterpvp.clans.world.discovery.Expedition;
import me.mykindos.betterpvp.clans.world.discovery.ExpeditionService;
import me.mykindos.betterpvp.clans.world.discovery.OceanPoint;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A drifting mound of ice, and the end of the voyage for anyone who does not steer around it.
 * <p>
 * Built out of {@link BlockDisplay} rather than blocks because it is repositioned on every tick: the mound is a fixed
 * spot on the plane, so following the projection is the only way it appears to drift past the hull.
 */
public class Iceberg extends Hazard {

    /** Enough lumps to read as a mass of ice from a distance without being a crowd of entities up close. */
    private static final int SHARD_COUNT = 16;

    /** Blocks out from the centre the mound spreads. */
    private static final double SPREAD = 4.0;

    /** One lump of the mound: where it sits relative to the centre, how big it is, and what it is made of. */
    @Value
    private static class Shard {
        double x;
        double y;
        double z;
        float scale;
        Material block;
    }

    /** How the archetype is registered, and everything one iceberg needs to end an expedition. */
    public static class Archetype implements HazardArchetype {

        private final HazardConfig config;
        private final EffectManager effects;
        private final Provider<ExpeditionService> expeditions;

        public Archetype(@NotNull HazardConfig config, @NotNull EffectManager effects,
                         @NotNull Provider<ExpeditionService> expeditions) {
            this.config = config;
            this.effects = effects;
            this.expeditions = expeditions;
        }

        @Override
        public @NotNull String key() {
            return "iceberg";
        }

        @Override
        public double radius() {
            return 6.0;
        }

        @Override
        public @NotNull Hazard create(@NotNull OceanPoint at) {
            return new Iceberg(this, at, config, effects, expeditions);
        }
    }

    private final HazardConfig config;
    private final EffectManager effects;
    private final Provider<ExpeditionService> expeditions;

    /** The mound's shape, rolled once so it does not reshuffle itself every tick. */
    private final List<Shard> shape = mound();

    private final List<BlockDisplay> shards = new ArrayList<>(SHARD_COUNT);

    private Iceberg(@NotNull Archetype archetype, @NotNull OceanPoint at, @NotNull HazardConfig config,
                    @NotNull EffectManager effects, @NotNull Provider<ExpeditionService> expeditions) {
        super(archetype, at);
        this.config = config;
        this.effects = effects;
        this.expeditions = expeditions;
    }

    @Override
    public void render(@NotNull Location at) {
        if (shards.isEmpty()) {
            spawn(at);
            return;
        }

        for (int i = 0; i < shards.size(); i++) {
            final BlockDisplay shard = shards.get(i);
            if (!shard.isValid()) {
                continue;
            }
            final Shard offset = shape.get(i);
            shard.teleport(at.clone().add(offset.getX(), offset.getY(), offset.getZ()));
        }
    }

    /**
     * Ends the voyage. The crew are put ashore by whoever picked them off the ice, still too shaken to say much about
     * how they got there.
     */
    @Override
    public void onCollide(@NotNull Expedition expedition) {
        final World limbo = Bukkit.getWorld(expedition.getLimbo().getWorldName());
        if (limbo != null) {
            // Before the recall, while they are still aboard: the return teleport is what they are meant to spend it on.
            for (Player sailor : limbo.getPlayers()) {
                effects.addEffect(sailor, EffectTypes.BLINDNESS, 1, config.getStrandedBlindnessMillis());
                effects.addEffect(sailor, EffectTypes.NAUSEA, 1, config.getStrandedNauseaMillis());

                // The hull itself giving way, loud and pitched right down.
                new SoundEffect(Sound.ENTITY_ITEM_BREAK, 0.5f, 3f).play(sailor);
                new SoundEffect(Sound.BLOCK_GLASS_BREAK, 0.5f, 1.4f).play(sailor);
                new SoundEffect(Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f).play(sailor);
            }
        }

        expeditions.get().strand(expedition);
    }

    @Override
    public void remove() {
        shards.forEach(BlockDisplay::remove);
        shards.clear();
    }

    private void spawn(@NotNull Location at) {
        final World world = at.getWorld();
        if (world == null) {
            return;
        }

        for (Shard shard : shape) {
            final Location where = at.clone().add(shard.getX(), shard.getY(), shard.getZ());
            shards.add(world.spawn(where, BlockDisplay.class, display -> {
                display.setBlock(shard.getBlock().createBlockData());
                display.setPersistent(false);

                // Without this a display that is repositioned every tick strobes between the two spots instead of
                // gliding between them, and the whole mound reads as a stutter rather than as something drifting.
                display.setTeleportDuration(1);

                final float scale = shard.getScale();
                display.setTransformation(new Transformation(
                        new Vector3f(-scale / 2f, -scale / 2f, -scale / 2f),
                        new AxisAngle4f(),
                        new Vector3f(scale),
                        new AxisAngle4f()));

                // It is meant to be seen and steered around from a long way off, which is further than a display
                // carries by default.
                UtilEntity.setViewRangeBlocks(display, 200f);
            }));
        }
    }

    /** A rough pile: wider lumps low and around the edge, smaller ones stacked toward the middle. */
    private static @NotNull List<Shard> mound() {
        final List<Shard> shape = new ArrayList<>(SHARD_COUNT);
        for (int i = 0; i < SHARD_COUNT; i++) {
            final double angle = UtilMath.randDouble(0, Math.PI * 2);
            final double spread = UtilMath.randDouble(0, SPREAD);
            final double height = (SPREAD - spread) * 0.9 + UtilMath.randDouble(-0.6, 0.6);

            shape.add(new Shard(Math.cos(angle) * spread, height, Math.sin(angle) * spread,
                    (float) UtilMath.randDouble(3.2, 6.2), ice()));
        }
        return shape;
    }

    private static @NotNull Material ice() {
        final double roll = UtilMath.randDouble(0, 1);
        if (roll < 0.5) {
            return Material.ICE;
        }
        return roll < 0.85 ? Material.PACKED_ICE : Material.BLUE_ICE;
    }
}
