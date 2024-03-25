package me.mykindos.betterpvp.core.client.punishments.types;

public class MutePunishmentType implements IPunishmentType {
    @Override
    public String getName() {
        return "Mute";
    }

    @Override
    public String getChatLabel() {
        return "muted";
    }

}
