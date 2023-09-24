package me.mykindos.betterpvp.clans.fields.model;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.gamer.GamerManager;

public abstract class CustomOre implements FieldsOre {

    protected final Clans clans;
    protected final GamerManager gamerManager;
    private double respawnDelay = 0;

    protected CustomOre(Clans clans, GamerManager gamerManager) {
        this.clans = clans;
        this.gamerManager = gamerManager;
    }

    @Override
    public final double getRespawnDelay() {
        return respawnDelay;
    }

    @Override
    public final void setRespawnDelay(double delay) {
        this.respawnDelay = delay;
    }

    protected <T> T getConfig(String name, Object defaultValue, Class<T> type) {
        final String key = "fields.blocks." + getName().toLowerCase().replace(" ", "") + "." + name;
        return clans.getConfig().getOrSaveObject(key, defaultValue, type);
    }

}
