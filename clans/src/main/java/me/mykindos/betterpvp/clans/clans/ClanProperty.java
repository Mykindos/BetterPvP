package me.mykindos.betterpvp.clans.clans;

import lombok.Getter;

@Getter
public enum ClanProperty {

    TIME_CREATED(true),
    LAST_LOGIN(true),
    EXPERIENCE(true),
    POINTS(true),
    ENERGY(true),
    NO_DOMINANCE_COOLDOWN(true),
    LAST_TNTED(true),
    BALANCE(true),
    TNT_PROTECTION(false);

    private final boolean saveProperty;

    ClanProperty(boolean saveProperty) {
        this.saveProperty = saveProperty;
    }

}
