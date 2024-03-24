package me.mykindos.betterpvp.core.client.punishments.types;

public class BuildLockPunishmentType implements IPunishmentType {

    @Override
    public String getName() {
        return "BuildLock";
    }

    @Override
    public String getChatLabel() {
        return "build locked";
    }

}
