package me.mykindos.betterpvp.core.client.punishments.types;

public class PvpLockPunishmentType implements IPunishmentType {
    @Override
    public String getName() {
        return "PVPLock";
    }

    @Override
    public String getChatLabel() {
        return "PVP locked";
    }

}
