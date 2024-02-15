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
    POISON(true),
    PROTECTION(false),
    PVPLOCK(false),
    RESISTANCE(false),
    INVISIBILITY(false),
    INVULNERABILITY(false),
    FRAILTY(true),
    TEXTURELOADING(false),
    IMMUNETOEFFECTS(false),
    CONFUSION(true),
    LEVITATION(true),
    NO_JUMP(false),
    NO_SPRINT(false),
    BLEED(true);

    final boolean isNegative;

    EffectType(boolean isNegative) {
        this.isNegative = isNegative;
    }
}
