package me.mykindos.betterpvp.core.scene.mob.sound;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.sound.Sound;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Ready-made {@link SoundProvider}s and combinators for the common sound-selection strategies - the
 * audio twin of {@link me.mykindos.betterpvp.core.scene.mob.animation.AnimationProviders}. These are
 * building blocks: they compose, so richer behaviour is expressed by nesting rather than by writing a
 * bespoke provider:
 * <pre>{@code
 * // A random hurt sound, each play pitched slightly differently so repeats don't sound canned.
 * setSound(MobSound.HURT, SoundProviders.withPitchVariation(
 *         SoundProviders.random(
 *                 new SoundEffect(Sound.ENTITY_RAVAGER_HURT, 0.9f),
 *                 new SoundEffect(Sound.ENTITY_RAVAGER_HURT, 1.1f)),
 *         0.15f));
 *
 * // A snarl while in combat, a softer grunt while idle.
 * setSound(MobSound.IDLE, SoundProviders.whenTargeting(
 *         SoundProviders.fixed(new SoundEffect(Sound.ENTITY_WOLF_GROWL, 1.0f)),
 *         SoundProviders.fixed(new SoundEffect(Sound.ENTITY_WOLF_AMBIENT, 0.8f))));
 * }</pre>
 */
public final class SoundProviders {

    private SoundProviders() {
    }

    /** Always resolves to the same effect - the equivalent of the plain {@link SoundEffect} mapping. */
    public static SoundProvider fixed(SoundEffect soundEffect) {
        Preconditions.checkNotNull(soundEffect, "soundEffect cannot be null");
        return mob -> soundEffect;
    }

    /** Resolves to a uniformly random effect from {@code soundEffects} on each request. */
    public static SoundProvider random(SoundEffect... soundEffects) {
        Preconditions.checkArgument(soundEffects.length > 0, "At least one sound effect must be provided");
        return mob -> soundEffects[UtilMath.randomInt(soundEffects.length)];
    }

    /**
     * Resolves to each effect in turn, wrapping back to the first after the last - useful for staged
     * sequences (a three-part roar). The cursor is per-provider state, so give each mob its own
     * instance (which {@code setSound} does naturally).
     */
    public static SoundProvider sequential(SoundEffect... soundEffects) {
        Preconditions.checkArgument(soundEffects.length > 0, "At least one sound effect must be provided");
        return new SoundProvider() {
            private int index = 0;

            @Override
            public SoundEffect resolve(SceneMob mob) {
                final SoundEffect soundEffect = soundEffects[index];
                index = (index + 1) % soundEffects.length;
                return soundEffect;
            }
        };
    }

    /**
     * Resolves to <em>all</em> of {@code soundEffects} played together as one cue, so a single
     * {@link MobSoundBehavior#play(MobSound)} fires a layered stack rather than one sound - a roar over
     * a low thud over a metallic screech. This is the one combinator that emits more than a single
     * sound per request; its behaviour has no {@link me.mykindos.betterpvp.core.scene.mob.animation.AnimationProvider}
     * analogue, since a mob can only run one animation clip at a time but Minecraft layers sound freely.
     * <pre>{@code
     * setSound(MobSound.DEATH, SoundProviders.layered(
     *         new SoundEffect(Sound.ENTITY_RAVAGER_DEATH, 0.8f),
     *         new SoundEffect(Sound.ENTITY_GENERIC_EXPLODE, 1.2f)));
     * }</pre>
     */
    public static SoundProvider layered(SoundEffect... soundEffects) {
        Preconditions.checkArgument(soundEffects.length > 0, "At least one sound effect must be provided");
        final SoundProvider[] providers = new SoundProvider[soundEffects.length];
        for (int i = 0; i < soundEffects.length; i++) {
            providers[i] = fixed(soundEffects[i]);
        }
        return layered(providers);
    }

    /**
     * Provider-composing form of {@link #layered(SoundEffect...)}: each layer is resolved against the
     * mob's live state at play time, then the non-null results are played together. Lets a layer carry
     * its own behaviour - a randomly-pitched random roar stacked under a fixed thud. A layer that
     * resolves to {@code null} simply drops out; if every layer is silent the cue plays nothing.
     * <p>
     * Layer last: wrapping a {@code layered} provider in {@link #withPitchVariation} would collapse it
     * to its first sound (pitch variation rebuilds a single effect), so apply pitch jitter to the
     * individual layers instead.
     */
    public static SoundProvider layered(SoundProvider... providers) {
        Preconditions.checkArgument(providers.length > 0, "At least one provider must be provided");
        for (SoundProvider provider : providers) {
            Preconditions.checkNotNull(provider, "providers cannot contain null");
        }
        return mob -> {
            final List<SoundEffect> resolved = new ArrayList<>(providers.length);
            for (SoundProvider provider : providers) {
                final SoundEffect effect = provider.resolve(mob);
                if (effect != null) {
                    resolved.add(effect);
                }
            }
            if (resolved.isEmpty()) {
                return null;
            }

            return resolved.size() == 1 ? resolved.getFirst() : SoundEffect.layered(resolved);
        };
    }

    /**
     * Picks between two providers based on the mob's live state at play time. Either branch may be
     * any provider, including another {@code when}, so conditions can be chained.
     */
    public static SoundProvider when(Predicate<SceneMob> condition, SoundProvider ifTrue, SoundProvider ifFalse) {
        Preconditions.checkNotNull(condition, "condition cannot be null");
        return mob -> (condition.test(mob) ? ifTrue : ifFalse).resolve(mob);
    }

    /** Single-effect shorthand for {@link #when(Predicate, SoundProvider, SoundProvider)}. */
    public static SoundProvider when(Predicate<SceneMob> condition, SoundEffect ifTrue, SoundEffect ifFalse) {
        return when(condition, fixed(ifTrue), fixed(ifFalse));
    }

    /**
     * Convenience over {@link #when}: uses {@code targeting} while the mob has a current target (i.e.
     * is in combat), otherwise {@code idle}. Covers the snarl/ambient, battle-cry/grunt style splits.
     */
    public static SoundProvider whenTargeting(SoundProvider targeting, SoundProvider idle) {
        return when(mob -> mob.getCurrentTarget() != null, targeting, idle);
    }

    /** Single-effect shorthand for {@link #whenTargeting(SoundProvider, SoundProvider)}. */
    public static SoundProvider whenTargeting(SoundEffect targeting, SoundEffect idle) {
        return whenTargeting(fixed(targeting), fixed(idle));
    }

    /**
     * Wraps any provider so the resolved effect is re-pitched by a random amount within
     * {@code ±spread} on each play. This is the one combinator with no animation analogue - it keeps
     * repeated cues (a fast-firing hurt sound) from sounding mechanically identical. The pitch is
     * clamped to Minecraft's playable {@code [0.5, 2.0]} range.
     */
    /** Single-effect shorthand for {@link #withPitchVariation(SoundProvider, float)}. */
    public static SoundProvider withPitchVariation(SoundEffect base, float spread) {
        return withPitchVariation(fixed(base), spread);
    }

    public static SoundProvider withPitchVariation(SoundProvider delegate, float spread) {
        Preconditions.checkNotNull(delegate, "delegate cannot be null");
        return mob -> {
            final SoundEffect base = delegate.resolve(mob);
            if (base == null) {
                return null;
            }
            final Sound sound = base.getSound();
            final float jitter = (UtilMath.RANDOM.nextFloat() * 2f - 1f) * spread;
            final float pitch = Math.max(0.5f, Math.min(2.0f, sound.pitch() + jitter));
            return new SoundEffect(Sound.sound(sound.name(), sound.source(), sound.volume(), pitch));
        };
    }

}
