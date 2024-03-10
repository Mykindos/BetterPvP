package me.mykindos.betterpvp.core.effects;

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
    private final boolean permanent;


    public Effect(String uuid, LivingEntity applier, EffectType effectType, String name, int amplifier, long length, boolean permanent) {
        this.uuid = uuid;
        this.applier = applier;
        this.effectType = effectType;
        this.name = name;
        this.rawLength = length + 50;
        this.length = System.currentTimeMillis() + length + 50;
        this.amplifier = amplifier;
        this.permanent = permanent;
    }

    public void setLength(long length) {
        this.length = System.currentTimeMillis() + length + 50;
        this.rawLength = length;
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
