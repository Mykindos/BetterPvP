package me.mykindos.betterpvp.core.client.punishments.types;

public class WarnPunishmentType implements IPunishmentType {
    @Override
    public String getName() {
        return "Warn";
    }

    @Override
    public String getChatLabel() {
        return "warned";
    }

    /**
     * Whether this punishment has a duration or not
     *
     * @return
     */
    @Override
    public boolean hasDuration() {
        return false;
    }
}
