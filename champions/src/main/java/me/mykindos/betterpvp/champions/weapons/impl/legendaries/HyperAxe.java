package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.utilities.ChampionsNamespacedKeys;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
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

import java.util.ArrayList;
import java.util.List;

@Singleton
@BPvPListener
public class HyperAxe extends Weapon implements InteractWeapon, LegendaryWeapon, Listener {

    private int damageDelay;
    private boolean dealsKnockback;
    private boolean usesEnergy;
    private int energyPerHit;
    private double hyperRushCooldown;
    private final EnergyHandler energyHandler;
    private final CooldownManager cooldownManager;
    private final EffectManager effectManager;

    @Inject
    public HyperAxe(Champions champions, EnergyHandler energyHandler, CooldownManager cooldownManager, EffectManager effectManager) {
        super(champions, "hyper_axe");
        this.energyHandler = energyHandler;
        this.cooldownManager = cooldownManager;
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(PreDamageEvent event) {
        if (!enabled) {
            return;
        }

        DamageEvent de = event.getDamageEvent();
        if (de.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(de.getDamager() instanceof Player player)) return;
        if (!isHoldingWeapon(player)) return;

        if (usesEnergy) {
            if (!energyHandler.use(player, "Hyper Axe", energyPerHit, true)) {
                return;
            }
        }

        if (de instanceof CustomDamageEvent cde) {
            cde.setKnockback(dealsKnockback);
        }
        de.setDamage(baseDamage);
        de.setDamageDelay(damageDelay);
    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Forged in the heart of a raging storm,", NamedTextColor.WHITE));
        lore.add(Component.text("this axe is known for its unparalleled speed.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(Component.text("Infused with the essence of a tempest,", NamedTextColor.WHITE));
        lore.add(Component.text("any wielder will tear through their opponents", NamedTextColor.WHITE));
        lore.add(Component.text("with unfathomable speed.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<white>Hit delay is reduced by <yellow>%.1f%%",  (100 - ((damageDelay / 400.0) * 100.0))));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f Damage <white>per hit", baseDamage));

        if (meta.getPersistentDataContainer().has(ChampionsNamespacedKeys.HYPER_AXE_SPEED) && meta.getPersistentDataContainer().has(ChampionsNamespacedKeys.HYPER_AXE_DURATION)) {
            lore.add(Component.text(""));
            int speedLevel = meta.getPersistentDataContainer().getOrDefault(ChampionsNamespacedKeys.HYPER_AXE_SPEED, PersistentDataType.INTEGER, 1);
            int duration = meta.getPersistentDataContainer().getOrDefault(ChampionsNamespacedKeys.HYPER_AXE_DURATION, PersistentDataType.INTEGER, 80);
            lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>Hyper Rush"));
            lore.add(UtilMessage.deserialize("<white>Gain <light_purple>Speed %s <white>for <green>%.2f seconds", UtilFormat.getRomanNumeral(speedLevel), duration / 20.0));
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
                effectManager.addEffect(player, EffectTypes.SPEED, level, (long) ((duration / 20d) * 1000));
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

    @Override
    public void loadWeaponConfig() {
        damageDelay = getConfig("damageDelay", 150, Integer.class);
        dealsKnockback = getConfig("dealsKnockback", true, Boolean.class);
        usesEnergy = getConfig("usesEnergy", false, Boolean.class);
        energyPerHit = getConfig("energyPerHit", 10, Integer.class);
        hyperRushCooldown = getConfig("hyperRushCooldown", 16.0, Double.class);
    }
}
