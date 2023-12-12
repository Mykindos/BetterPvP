package me.mykindos.betterpvp.clans.fields.model;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.client.repository.ClientManager;

public abstract class CustomOre implements FieldsOre {

    protected final Clans clans;
    protected final ClientManager clientManager;
    private double respawnDelay = 0;

    protected CustomOre(Clans clans, ClientManager clientManager) {
        this.clans = clans;
        this.clientManager = clientManager;
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
