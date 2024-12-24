package me.mykindos.betterpvp.core.effects;

import com.google.inject.Inject;
import lombok.Data;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

@Data
public abstract class EffectType {

    private Material material;
    private int modelData;

    public abstract String getName();
    public abstract boolean isNegative();

    /**
     * If true, players can receive multiple instances of this effect
     * If false and a player receives this effect, it will replace the current effect
     * @return True if the effect can stack
     */
    public boolean canStack() {
        return true;
    }

    public int defaultAmplifier() {
        return 1;
    }

    public void onReceive(LivingEntity livingEntity, Effect effect) {

    }

    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {

    }

    public void onTick(LivingEntity livingEntity, Effect effect) {

    }

    public String getDescription(int level) {
        return "";
    }

    public String getGenericDescription() {
        return "";
    }

    public boolean mustBeManuallyRemoved() {
        return false;
    }

    protected <T> T getConfig(String name, Object defaultValue, Class<T> type, BPvPPlugin plugin) {
        String path = getName().toLowerCase().replace(" ", "") + "." + name;
        return plugin.getConfig("effects/effects").getOrSaveObject(path, defaultValue, type);
    }

    public final void loadConfig(BPvPPlugin plugin) {
        String materialString = getConfig("material", "PAPER", String.class, plugin);
        modelData = getConfig("modelData", 0, Integer.class, plugin);

        material = Material.matchMaterial(materialString);
        if (material == null) {
            material = Material.PAPER;
        }
        loadEffectConfig(plugin);
    }

    protected void loadEffectConfig(BPvPPlugin plugin) {

    }
}
