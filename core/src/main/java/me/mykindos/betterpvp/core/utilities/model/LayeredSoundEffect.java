package me.mykindos.betterpvp.core.utilities.model;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;

import java.util.List;

/**
 * A {@link SoundEffect} that <em>is</em> several effects: every play path fans out to each member, so a consumer that
 * plays a single resolved effect emits the whole stack from one cue without any awareness that layering is happening.
 * It reports its first member's {@link Sound} for {@link #getSound()} so single-sound consumers still see a sensible
 * value.
 * <p>
 * Build one via {@link SoundEffect#layered(SoundEffect...)}.
 */
public final class LayeredSoundEffect extends SoundEffect {

    private final List<SoundEffect> effects;

    LayeredSoundEffect(List<SoundEffect> effects) {
        super(effects.getFirst().getSound());
        this.effects = List.copyOf(effects);
    }

    @Override
    public void play(Location location) {
        effects.forEach(effect -> effect.play(location));
    }

    @Override
    public void play(Audience audience) {
        effects.forEach(effect -> effect.play(audience));
    }

    @Override
    public void play(Audience audience, Location location) {
        effects.forEach(effect -> effect.play(audience, location));
    }

    @Override
    public void play(Audience audience, Sound.Emitter emitter) {
        effects.forEach(effect -> effect.play(audience, emitter));
    }

    @Override
    public void broadcast() {
        effects.forEach(SoundEffect::broadcast);
    }

    @Override
    public void broadcast(Sound.Emitter emitter) {
        effects.forEach(effect -> effect.broadcast(emitter));
    }
}
