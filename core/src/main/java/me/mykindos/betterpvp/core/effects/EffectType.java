package me.mykindos.betterpvp.core.effects;

import lombok.Getter;

@Getter
public enum EffectType {

    SILENCE(true),
    VULNERABILITY(true),
    STUN(true),
    SHOCK(true),
    STRENGTH(false),
    NOFALL(false),
    SHARD_50(false),
    SHARD_100(false),
    POISON(true),
    PROTECTION(false),
    PVPLOCK(false),
    RESISTANCE(false),
    INVISIBILITY(false),
    INVULNERABILITY(false),
    FRAILTY(true),
    TEXTURELOADING(false),
    IMMUNETOEFFECTS(false),
    LEVITATION(true);

    final boolean isNegative;

    EffectType(boolean isNegative) {
        this.isNegative = isNegative;
    }
}
