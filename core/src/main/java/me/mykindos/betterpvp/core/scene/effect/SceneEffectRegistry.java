package me.mykindos.betterpvp.core.scene.effect;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Turns a parsed {@link ScriptSpec} into a playable {@link SceneEffect}, by kind.
 * <p>
 * The kinds below ship with core; any module may add its own with {@link #register}, which is the one
 * extension point - a new cue type is a lambda, not a class, and every caller that already fires
 * effects picks it up without changing.
 *
 * <h3>sound</h3>
 * {@code sound;s=block.gravel.step;v=0.4;p=1.1} - {@code s|sound|key} is a sound key (namespace
 * optional), {@code v|vol|volume} and {@code p|pitch} default to 1.
 *
 * <h3>particle</h3>
 * {@code particle;t=flame;c=5;o=0.3;sp=0.01;r=60} - {@code t|type|particle} names the particle,
 * {@code c|count} how many, {@code o|offset} the random spread (one number spreads evenly on all
 * three axes), {@code sp|speed} the particle's extra data value, {@code r|range|receivers} how far
 * away it is visible, and {@code force} shows it regardless of the viewer's particle setting.
 * Particles that need data take {@code color=255,0,0} with {@code size}, or {@code material=stone}.
 * <p>
 * Note that an effect is played once per origin by whatever fires it, so a count is per place, not
 * per instruction - see {@code ScriptEffectBehavior}'s multi-bone anchoring.
 */
@Singleton
@CustomLog
public class SceneEffectRegistry {

    /** Default radius, in blocks, within which a particle is sent. Matches vanilla's own cutoff. */
    private static final int DEFAULT_RECEIVER_RADIUS = 32;

    private final Map<String, Function<ScriptSpec, SceneEffect>> factories = new ConcurrentHashMap<>();

    public SceneEffectRegistry() {
        register("sound", SceneEffectRegistry::sound);
        register("particle", SceneEffectRegistry::particle);
    }

    /**
     * Adds or replaces the factory for one kind.
     *
     * @param kind    the leading word of the instruction, e.g. {@code "particle"}
     * @param factory builds the effect from its parameters; may throw
     *                {@link IllegalArgumentException} for parameters it cannot use
     */
    public void register(@NotNull String kind, @NotNull Function<ScriptSpec, SceneEffect> factory) {
        factories.put(kind.toLowerCase(Locale.ROOT), factory);
    }

    /** @return whether any factory claims {@code kind}. */
    public boolean knows(@NotNull String kind) {
        return factories.containsKey(kind.toLowerCase(Locale.ROOT));
    }

    /**
     * @return the effect described by {@code spec}, or {@code null} if no factory claims its kind or
     * the parameters are unusable - a broken instruction is reported and skipped, never thrown at the
     * animation that fired it
     */
    @Nullable
    public SceneEffect create(@NotNull ScriptSpec spec) {
        final Function<ScriptSpec, SceneEffect> factory = factories.get(spec.kind());
        if (factory == null) {
            return null;
        }
        try {
            return factory.apply(spec);
        } catch (RuntimeException exception) {
            log.warn("Effect instruction '{}' could not be read: {}", spec.kind(), exception.getMessage()).submit();
            return null;
        }
    }

    private static SceneEffect sound(ScriptSpec spec) {
        final String key = spec.get("s", "sound", "key")
                .orElseThrow(() -> new IllegalArgumentException("no sound key ('s')"));
        final SoundEffect effect = new SoundEffect(Sound.sound(
                Key.key(key.toLowerCase(Locale.ROOT)),
                Sound.Source.MASTER,
                (float) spec.getDouble(1, "v", "vol", "volume"),
                (float) spec.getDouble(1, "p", "pitch")));
        return effect::play;
    }

    private static SceneEffect particle(ScriptSpec spec) {
        final String name = spec.get("t", "type", "particle")
                .orElseThrow(() -> new IllegalArgumentException("no particle type ('t')"));
        final Particle particle = particleType(name);
        final Vector spread = spec.getVector(new Vector(), "o", "offset");
        final int count = spec.getInt(1, "c", "count");
        final double speed = spec.getDouble(0, "sp", "speed", "extra");
        final int radius = spec.getInt(DEFAULT_RECEIVER_RADIUS, "r", "range", "receivers");
        final boolean force = spec.getBoolean(false, "force");
        final Object data = particleData(particle, spec);

        return origin -> particle.builder()
                .location(origin)
                .receivers(radius)
                .count(count)
                .offset(spread.getX(), spread.getY(), spread.getZ())
                .extra(speed)
                .force(force)
                .data(data)
                .spawn();
    }

    private static Particle particleType(String name) {
        final String key = name.toLowerCase(Locale.ROOT);
        final Particle particle = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(key));
        if (particle != null) {
            return particle;
        }
        // Fall back to the enum name: a handful of Bukkit constants do not match their registry key.
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("'" + name + "' is not a particle");
        }
    }

    /**
     * Builds the extra value particles like dust and block break cannot spawn without.
     *
     * @return the data object, or {@code null} for particles that take none
     */
    @Nullable
    private static Object particleData(Particle particle, ScriptSpec spec) {
        final Class<?> type = particle.getDataType();
        if (type == Void.class) {
            return null;
        }
        if (type == Particle.DustOptions.class) {
            return new Particle.DustOptions(color(spec), (float) spec.getDouble(1, "size", "scale"));
        }
        if (type == BlockData.class) {
            return material(spec).createBlockData();
        }
        if (type == ItemStack.class) {
            return new ItemStack(material(spec));
        }
        throw new IllegalArgumentException(particle.name() + " needs " + type.getSimpleName() + " data, which is not supported");
    }

    private static Color color(ScriptSpec spec) {
        final Vector rgb = spec.getVector(new Vector(255, 255, 255), "color", "col", "rgb");
        return Color.fromRGB((int) rgb.getX(), (int) rgb.getY(), (int) rgb.getZ());
    }

    private static Material material(ScriptSpec spec) {
        final String name = spec.get("material", "mat", "block", "item")
                .orElseThrow(() -> new IllegalArgumentException("no 'material'"));
        final Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        if (material == null) {
            throw new IllegalArgumentException("'" + name + "' is not a material");
        }
        return material;
    }
}
