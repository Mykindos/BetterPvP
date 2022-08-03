package me.mykindos.betterpvp.core.effects;

import lombok.Data;

@Data
public class Effect {
    private final String uuid;
    private final EffectType effectType;
    private final long length;
    private final long rawLength;
    private int level;

    public Effect(String uuid, EffectType effectType, long length) {
        this.uuid = uuid;
        this.effectType = effectType;
        this.rawLength = length;
        this.length = System.currentTimeMillis() + length;
    }

    public Effect(String uuid, EffectType effectType, int level, long length) {
        this.uuid = uuid;
        this.effectType = effectType;
        this.rawLength = length;
        this.length = System.currentTimeMillis() + length;
        this.level = level;
    }

    public boolean hasExpired() {
        return length - System.currentTimeMillis() <= 0;
    }
}
