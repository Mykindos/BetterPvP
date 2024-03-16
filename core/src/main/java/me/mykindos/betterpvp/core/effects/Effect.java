package me.mykindos.betterpvp.core.effects;

import lombok.Data;
import org.bukkit.entity.LivingEntity;

import java.util.function.Predicate;

@Data
public class Effect {

    private final String uuid;
    private LivingEntity applier;
    private final EffectType effectType;
    private final String name;
    private long length;
    private long rawLength;
    private int amplifier;
    private final boolean permanent;
    private Predicate<LivingEntity> removalPredicate;


    public Effect(String uuid, LivingEntity applier, EffectType effectType, String name, int amplifier, long length, boolean permanent, Predicate<LivingEntity> removalPredicate) {
        this.uuid = uuid;
        this.applier = applier;
        this.effectType = effectType;
        this.name = name;
        this.rawLength = length + 50;
        this.length = System.currentTimeMillis() + length + 50;
        this.amplifier = amplifier;
        this.permanent = permanent;
        this.removalPredicate = removalPredicate;
    }

    public void setLength(long length) {
        this.rawLength = length + 50;
        this.length = System.currentTimeMillis() + length + 50;
    }

    public boolean hasExpired() {
        return rawLength >= 0 && length - System.currentTimeMillis() <= 0 && !permanent;
    }

    public long getRemainingDuration() {
        return length - System.currentTimeMillis();
    }

    public int getVanillaDuration() {
        return permanent ? -1 : (int) Math.ceil((rawLength / 1000d) * 20d);
    }

    public int getRemainingVanillaDuration() {
        return permanent ? -1 : (int) Math.ceil((getRemainingDuration() / 1000d) * 20d);
    }
}
