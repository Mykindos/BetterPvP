package me.mykindos.betterpvp.core.client.punishments;

import me.mykindos.betterpvp.core.client.punishments.types.BanPunishmentType;
import me.mykindos.betterpvp.core.client.punishments.types.BuildLockPunishmentType;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.client.punishments.types.MutePunishmentType;
import me.mykindos.betterpvp.core.client.punishments.types.PvpLockPunishmentType;

import java.util.HashSet;
import java.util.Set;

public final class PunishmentTypes {


    private PunishmentTypes() {}

    private static final Set<IPunishmentType> PUNISHMENT_TYPES = new HashSet<>();

    public static final IPunishmentType BAN = registerPunishmentType(new BanPunishmentType());
    public static final IPunishmentType MUTE = registerPunishmentType(new MutePunishmentType());
    public static final IPunishmentType PVP_LOCK = registerPunishmentType(new PvpLockPunishmentType());
    public static final IPunishmentType BUILD_LOCK = registerPunishmentType(new BuildLockPunishmentType());

    public static IPunishmentType registerPunishmentType(IPunishmentType type) {
        PUNISHMENT_TYPES.add(type);
        return type;
    }

    public static IPunishmentType getPunishmentType(String name) {
        return PUNISHMENT_TYPES.stream().filter(type -> type.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public static Set<IPunishmentType> getPunishmentTypes() {
        return PUNISHMENT_TYPES;
    }

}
