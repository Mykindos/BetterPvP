package me.mykindos.betterpvp.core.framework;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Loader {

    protected BPvPPlugin plugin;
    protected int count;

    public Loader(BPvPPlugin plugin) {
        this.plugin = plugin;
    }

    public abstract void load(Class<?> clazz);

    public void reload(String packageName) {}

}
