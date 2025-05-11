package me.mykindos.betterpvp.core.effects;

import java.util.function.Predicate;
import lombok.Data;
import org.bukkit.entity.LivingEntity;

@Data
public class Effect {

    private final String uuid;
    private LivingEntity applier;
    private final EffectType effectType;
    private final String name;
    private long length;
    private long rawLength;
    private int amplifier;
    private boolean permanent;
    private boolean showParticles;
    private Predicate<LivingEntity> removalPredicate;


    /**
     * Constructs a new Effect instance with the given parameters.
     *
     * @param uuid the unique identifier for this effect
     * @param applier the entity that applied this effect
     * @param effectType the type of the effect
     * @param name the name of the effect
     * @param amplifier the amplifier level of the effect
     * @param length the duration of the effect in milliseconds
     * @param permanent indicates whether the effect is permanent
     * @param showParticles indicates whether particles should be shown while the effect is active
     * @param removalPredicate a predicate that determines whether the effect should be removed based on the entity's state
     */
    public Effect(String uuid, LivingEntity applier, EffectType effectType, String name, int amplifier, long length, boolean permanent, boolean showParticles, Predicate<LivingEntity> removalPredicate) {
        this.uuid = uuid;
        this.applier = applier;
        this.effectType = effectType;
        this.name = name;
        this.rawLength = length + 50;
        this.length = System.currentTimeMillis() + length + 50;
        this.amplifier = amplifier;
        this.permanent = permanent;
        this.showParticles = showParticles;
        this.removalPredicate = removalPredicate;
    }

    /**
     * Updates the length of the effect. The raw length is adjusted by adding 50
     * milliseconds to the given value, and the length is set to the current system
     * time plus the given value plus 50 milliseconds.
     *
     * @param length the base length of the effect in milliseconds
     */
    public void setLength(long length) {
        this.rawLength = length + 50;
        this.length = System.currentTimeMillis() + length + 50;
    }

    /**
     * Determines whether the effect has expired based on its length, current time,
     * and whether it is marked as permanent.
     *
     * @return true if the effect is not permanent, its raw length is non-negative,
     *         and the remaining time has elapsed; false otherwise.
     */
    public boolean hasExpired() {
        return rawLength >= 0 && length - System.currentTimeMillis() <= 0 && !permanent;
    }

    /**
     * Calculates and returns the remaining duration of the effect in milliseconds.
     * The remaining duration is computed by subtracting the current system time
     * in milliseconds from the length of the effect.
     *
     * @return the remaining duration of the effect in milliseconds
     */
    public long getRemainingDuration() {
        return length - System.currentTimeMillis();
    }

    /**
     * Calculates and returns the "vanilla" duration of the effect, measured in ticks.
     * The vanilla duration is computed based on the raw length of the effect's duration.
     * If the effect is permanent, it returns -1.
     *
     * @return the vanilla duration in ticks, or -1 if the effect is permanent
     */
    public int getVanillaDuration() {
        return permanent ? -1 : (int) Math.ceil((rawLength / 1000d) * 20d);
    }

    /**
     * Calculates the remaining duration of the effect in vanilla ticks.
     * If the effect is permanent, returns -1.
     * The conversion to vanilla ticks assumes 20 ticks per second.
     *
     * @return The remaining duration in vanilla ticks, or -1 if the effect is permanent.
     */
    public int getRemainingVanillaDuration() {
        return permanent ? -1 : (int) Math.ceil((getRemainingDuration() / 1000d) * 20d);
    }
}
