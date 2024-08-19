package me.mykindos.betterpvp.core.tips;

import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
@Getter
public abstract class Tip {

    BPvPPlugin plugin;

    private Boolean enabled;
    private int categoryWeight;
    private int weight;

    private final int defaultCategoryWeight;
    private final int defaultWeight;

    @Setter
    private Component component;

    protected Tip(BPvPPlugin plugin, int defaultCategoryWeight, int defaultWeight, Component component) {
        this.plugin = plugin;
        this.defaultCategoryWeight = defaultCategoryWeight;
        this.defaultWeight = defaultWeight;
        this.component = component;
        loadConfig(plugin);
    }

    protected Tip(BPvPPlugin plugin, int defaultCategoryWeight, int defaultWeight) {
        this(plugin, defaultCategoryWeight, defaultWeight, Component.empty());
    }


    public abstract String getName();

    public boolean isValid(Player player) {
        return false;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public Component generateComponent() {
        return Component.empty();
    }

    protected <T> T getConfig(String name, Object defaultValue, Class<T> type) {
        String path  = "tips." + getName().toLowerCase().replace(" ", "") + "." + name;
        return this.plugin.getConfig().getOrSaveObject(path, defaultValue, type);
    }

    public final void loadConfig(BPvPPlugin plugin) {
        if (!this.getPlugin().equals(plugin)) {
            return;
        }
        enabled = getConfig("enabled", true, Boolean.class);
        categoryWeight = getConfig("categoryWeight", this.defaultCategoryWeight, Integer.class);
        weight = getConfig("weight", this.defaultWeight, Integer.class);
        loadTipConfig();
    }

    public void loadTipConfig() {
        //allows for the possibility of being overridden
    }

}
