package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.utilities.ChampionsNamespacedKeys;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

@Singleton
@BPvPListener
public class HyperAxe extends Weapon implements InteractWeapon, LegendaryWeapon, Listener {

    @Inject
    @Config(path = "weapons.hyper-axe.damageDelay", defaultValue = "200", configName = "weapons/legendaries")
    private int damageDelay;

    @Inject
    @Config(path = "weapons.hyper-axe.baseDamage", defaultValue = "4.0", configName = "weapons/legendaries")
    private double baseDamage;

    @Inject
    @Config(path = "weapons.hyper-axe.dealsKnockback", defaultValue = "false", configName = "weapons/legendaries")
    private boolean dealsKnockback;

    @Inject
    @Config(path = "weapons.hyper-axe.usesEnergy", defaultValue = "false", configName = "weapons/legendaries")
    private boolean usesEnergy;

    @Inject
    @Config(path = "weapons.hyper-axe.energyPerHit", defaultValue = "10", configName = "weapons/legendaries")
    private int energyPerHit;

    @Inject
    @Config(path = "weapons.hyper-axe.hyperRushCooldown", defaultValue = "16", configName = "weapons/legendaries")
    private double hyperRushCooldown;

    private final EnergyHandler energyHandler;
    private final CooldownManager cooldownManager;

    @Inject
    public HyperAxe(EnergyHandler energyHandler, CooldownManager cooldownManager) {
        super("hyper_axe");
        this.energyHandler = energyHandler;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(CustomDamageEvent event) {

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!isHoldingWeapon(player)) return;

        if (usesEnergy) {
            if (!energyHandler.use(player, "Hyper Axe", energyPerHit, true)) {
                return;
            }
        }

        event.setDamage(baseDamage);
        event.setKnockback(dealsKnockback);
        event.setDamageDelay(damageDelay);

    }

    @Override
    public List<Component> getLore(ItemStack item) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Forged in the heart of a raging storm,", NamedTextColor.WHITE));
        lore.add(Component.text("this axe is known for its unparalleled speed.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(Component.text("Infused with the essence of a tempest,", NamedTextColor.WHITE));
        lore.add(Component.text("any wielder will tear through their opponents", NamedTextColor.WHITE));
        lore.add(Component.text("with unfathomable speed.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<white>Hit delay is reduced by <yellow>%.1f%%", ((damageDelay / 400.0) * 100.0)));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f Damage <white>per hit", baseDamage));

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.getPersistentDataContainer().has(ChampionsNamespacedKeys.HYPER_AXE_SPEED) && meta.getPersistentDataContainer().has(ChampionsNamespacedKeys.HYPER_AXE_DURATION)) {
                lore.add(Component.text(""));
                int speedLevel = meta.getPersistentDataContainer().getOrDefault(ChampionsNamespacedKeys.HYPER_AXE_SPEED, PersistentDataType.INTEGER, 1);
                int duration = meta.getPersistentDataContainer().getOrDefault(ChampionsNamespacedKeys.HYPER_AXE_DURATION, PersistentDataType.INTEGER, 80);
                lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>Hyper Rush"));
                lore.add(UtilMessage.deserialize("<white>Gain <light_purple>Speed %s <white>for <green>%.2f seconds", UtilFormat.getRomanNumeral(speedLevel), duration / 20.0));
            }
        }

        return lore;
    }

    @Override
    public void onInitialize(ItemMeta meta) {

        if (!meta.getPersistentDataContainer().has(ChampionsNamespacedKeys.HYPER_AXE_SPEED)) {
            int level = UtilMath.randomInt(1, 5);
            meta.getPersistentDataContainer().set(ChampionsNamespacedKeys.HYPER_AXE_SPEED, PersistentDataType.INTEGER, level);
        }

        if (!meta.getPersistentDataContainer().has(ChampionsNamespacedKeys.HYPER_AXE_DURATION)) {
            int duration = UtilMath.randomInt(80, 320);
            meta.getPersistentDataContainer().set(ChampionsNamespacedKeys.HYPER_AXE_DURATION, PersistentDataType.INTEGER, duration);
        }

    }

    @Override
    public void activate(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(ChampionsNamespacedKeys.HYPER_AXE_SPEED) && meta.getPersistentDataContainer().has(ChampionsNamespacedKeys.HYPER_AXE_DURATION)) {

            int level = meta.getPersistentDataContainer().getOrDefault(ChampionsNamespacedKeys.HYPER_AXE_SPEED, PersistentDataType.INTEGER, 1);
            int duration = meta.getPersistentDataContainer().getOrDefault(ChampionsNamespacedKeys.HYPER_AXE_DURATION, PersistentDataType.INTEGER, 80);
            if (cooldownManager.use(player, "Hyper Rush", hyperRushCooldown, true)) {
                UtilMessage.simpleMessage(player, "Hyper Axe", "You used <green>Hyper Rush<gray>.");
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, level - 1));
                UtilSound.playSound(player.getWorld(), player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1);
            }
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (UtilBlock.isInWater(player)) {
            UtilMessage.simpleMessage(player, "Hyper Axe", "You cannot use <green>Hyper Rush <gray>in water.");
            return false;
        }
        return true;
    }
}
