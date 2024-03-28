package me.mykindos.betterpvp.core.combat.weapon;

import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.items.BPvPItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@CustomLog
@Getter
@Singleton
public abstract class Weapon extends BPvPItem implements IWeapon {

    private final BPvPPlugin plugin;
    protected boolean enabled;
    protected double energyPerTick;
    protected double initialEnergyCost;
    protected double baseDamage;
    protected double cooldown;

    protected Weapon(BPvPPlugin plugin, String key) {
        this(key, plugin, null);
    }

    protected Weapon(String key, BPvPPlugin plugin, List<Component> lore) {
        this(plugin, key, lore, "champions");
    }

    protected Weapon(BPvPPlugin plugin, String key, String namespace) {
        this(plugin, key, null, namespace);
    }

    protected Weapon(BPvPPlugin plugin, String key, List<Component> lore, String namespace) {
        super(namespace, key, Material.DEBUG_STICK, Component.text("Uninitialized Weapon"), lore, 0, 2, false, true);
        this.plugin = plugin;
        loadConfig(plugin);
    }

    public void loadWeapon(BPvPItem item) {
        setMaterial(item.getMaterial());
        setName(item.getName());
        setLore(item.getLore());
        setMaxDurability(item.getMaxDurability());
        setCustomModelData(item.getCustomModelData());
        setGlowing(item.isGlowing());
        setGiveUUID(item.isGiveUUID());
    }

    /**
     * @param name         name of the value
     * @param defaultValue default value
     * @param type         The type of default value
     * @param <T>          The type of default value
     * @return returns the config value if exists, or the default value if it does not. Saves the default value if no value exists
     */
    protected <T> T getConfig(String name, Object defaultValue, Class<T> type) {
        return plugin.getConfig(getConfigName()).getOrSaveObject(getPath(name), defaultValue, type);
    }

    /**
     * @param name         name of the value
     * @param defaultValue default value
     * @param type         The type of default value
     * @param <T>          The type of default value
     * @return returns the config value if exists, or the default value if it does not. Does not save value in the config
     */
    protected <T> T getConfigObject(String name, T defaultValue, Class<T> type) {
        return plugin.getConfig(getConfigName()).getObject(getPath(name), type, defaultValue);
    }

    protected String getPath(String name) {
        return getKey() + "." + name;
    }

    public void loadConfig(BPvPPlugin plugin) {
        if (!this.getPlugin().equals(plugin)) {
            //only reload/load config when called by the correct plugin
            return;
        }
        enabled = getConfig("enabled", true, Boolean.class);
        if (this instanceof LegendaryWeapon) {
            baseDamage = getConfig("baseDamage", 8.0, Double.class);
        }
        if (this instanceof ChannelWeapon) {
            energyPerTick = getConfig("energyPerTick", 1.0, Double.class);
            initialEnergyCost = getConfig("initialEnergyCost", 10.0, Double.class);
        }
        if (this instanceof CooldownWeapon) {
            cooldown = getConfig("cooldown", 10.0, Double.class);
        }

        loadWeaponConfig();
        plugin.saveConfig();
    }

    public void loadWeaponConfig() {
    }

    @Override
    public boolean isHoldingWeapon(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        return matches(itemStack);
    }

    public int getModel() {
        return getCustomModelData();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
