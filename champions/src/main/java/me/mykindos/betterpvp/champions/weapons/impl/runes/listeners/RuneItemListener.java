package me.mykindos.betterpvp.champions.weapons.impl.runes.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.weapons.impl.runes.Rune;
import me.mykindos.betterpvp.champions.weapons.impl.runes.RuneNamespacedKeys;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.cooldowns.events.CooldownEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.energy.events.DegenerateEnergyEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEffect;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

@BPvPListener
@Singleton
public class RuneItemListener implements Listener {

    private final Champions champions;
    private final ItemHandler itemHandler;
    private final EffectManager effectManager;

    @Inject
    public RuneItemListener(Champions champions, ItemHandler itemHandler, EffectManager effectManager) {
        this.champions = champions;
        this.itemHandler = itemHandler;
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onConqueringDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        ItemStack mainHand = damager.getInventory().getItemInMainHand();
        ItemMeta itemMeta = mainHand.getItemMeta();

        Rune rune = getRuneFromNamespacedKey(RuneNamespacedKeys.CONQUERING, itemMeta);
        if (rune == null) return;

        PersistentDataContainer conqueringPdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        if (conqueringPdc != null) {
            event.setDamage(event.getDamage() + rune.getRollFromItem(conqueringPdc, rune.getAppliedNamespacedKey(), PersistentDataType.DOUBLE));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHasteSpeed(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        ItemStack mainHand = damager.getInventory().getItemInMainHand();
        ItemMeta itemMeta = mainHand.getItemMeta();

        Rune rune = getRuneFromNamespacedKey(RuneNamespacedKeys.HASTE, itemMeta);
        if (rune == null) return;

        PersistentDataContainer hastePdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        if (hastePdc != null) {
            event.setDamageDelay((long) (event.getDamageDelay() * (1 - rune.getRollFromItem(hastePdc, rune.getAppliedNamespacedKey(), PersistentDataType.DOUBLE) / 100)));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrost(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        ItemStack mainHand = damager.getInventory().getItemInMainHand();
        ItemMeta itemMeta = mainHand.getItemMeta();

        Rune rune = getRuneFromNamespacedKey(RuneNamespacedKeys.FROST, itemMeta);
        if (rune == null) return;

        PersistentDataContainer frostPdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        if (frostPdc != null) {
            double chance = rune.getRollFromItem(frostPdc, RuneNamespacedKeys.FROST_CHANCE, PersistentDataType.DOUBLE);
            double duration = rune.getRollFromItem(frostPdc, RuneNamespacedKeys.FROST_DURATION, PersistentDataType.DOUBLE);
            int amplifier = rune.getRollFromItem(frostPdc, RuneNamespacedKeys.FROST_AMPLIFIER, PersistentDataType.INTEGER);

            if (UtilMath.randDouble(0, 100) <= chance) {
                effectManager.addEffect(event.getDamagee(), damager, EffectTypes.SLOWNESS, amplifier, (long) duration * 1000L);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onScorching(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if(!(event.getProjectile() instanceof Arrow)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) return;

        ItemStack mainHand = damager.getInventory().getItemInMainHand();
        ItemMeta itemMeta = mainHand.getItemMeta();

        Rune rune = getRuneFromNamespacedKey(RuneNamespacedKeys.SCORCHING, itemMeta);
        if (rune == null) return;

        PersistentDataContainer scorchingPdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        if (scorchingPdc != null) {
            double chance = rune.getRollFromItem(scorchingPdc, RuneNamespacedKeys.SCORCHING_CHANCE, PersistentDataType.DOUBLE);
            double duration = rune.getRollFromItem(scorchingPdc, RuneNamespacedKeys.SCORCHING_DURATION, PersistentDataType.DOUBLE);

            double v = UtilMath.randDouble(0, 100);
            if (v <= chance) {
                UtilServer.runTaskLater(champions, () -> UtilEntity.setFire(event.getDamagee(), event.getDamager(), (long) (1000L * duration)), 2);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onReinforced(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE
                && event.getCause() != EntityDamageEvent.DamageCause.CUSTOM) {
            return;
        }

        ItemStack[] armour = player.getInventory().getArmorContents();

        double totalReduction = getSumOfRuneEffect(armour, RuneNamespacedKeys.REINFORCING);

        event.setDamage(event.getDamage() * (1 - totalReduction / 100));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPowerDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getProjectile() instanceof Arrow)) return;

        ItemStack mainHand = damager.getInventory().getItemInMainHand();
        if (mainHand.getType() != Material.BOW && mainHand.getType() != Material.CROSSBOW) return;

        ItemMeta itemMeta = mainHand.getItemMeta();

        Rune rune = getRuneFromNamespacedKey(RuneNamespacedKeys.POWER, itemMeta);
        if (rune == null) return;

        PersistentDataContainer powerPdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        if (powerPdc != null) {
            event.setDamage(event.getDamage() + rune.getRollFromItem(powerPdc, rune.getAppliedNamespacedKey(), PersistentDataType.DOUBLE));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInsightEnergy(DegenerateEnergyEvent event) {
        Player player = event.getPlayer();
        ItemStack[] armour = player.getInventory().getArmorContents();

        double totalReduction = 0;
        for (ItemStack item : armour) {
            if (item == null) continue;
            ItemMeta itemMeta = item.getItemMeta();
            Rune rune = getRuneFromNamespacedKey(RuneNamespacedKeys.INSIGHT, itemMeta);
            if (rune == null) continue;

            PersistentDataContainer insightPdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
            if (insightPdc != null) {
                totalReduction += rune.getRollFromItem(insightPdc, rune.getAppliedNamespacedKey(), PersistentDataType.DOUBLE);
            }

        }

        event.setEnergy(event.getEnergy() * (1 - totalReduction / 100));

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAlacrityCooldown(CooldownEvent event) {
        Player player = event.getPlayer();
        ItemStack[] armour = player.getInventory().getArmorContents();

        double totalReduction = 0;
        for (ItemStack item : armour) {
            if (item == null) continue;
            ItemMeta itemMeta = item.getItemMeta();
            Rune rune = getRuneFromNamespacedKey(RuneNamespacedKeys.ALACRITY, itemMeta);
            if (rune == null) continue;

            PersistentDataContainer alacrityPdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
            if (alacrityPdc != null) {
                totalReduction += rune.getRollFromItem(alacrityPdc, rune.getAppliedNamespacedKey(), PersistentDataType.DOUBLE);
            }

        }

        event.getCooldown().setSeconds(event.getCooldown().getSeconds() * (1 - totalReduction / 100));

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDurability(PlayerItemDamageEvent event) {
        ItemMeta itemMeta = event.getItem().getItemMeta();

        Rune rune = getRuneFromNamespacedKey(RuneNamespacedKeys.UNBREAKING, itemMeta);
        if (rune == null) return;

        PersistentDataContainer unbreakingPdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        if (unbreakingPdc != null) {
            double ignoreChance = rune.getRollFromItem(unbreakingPdc, rune.getAppliedNamespacedKey(), PersistentDataType.DOUBLE);
            if (UtilMath.randDouble(0, 100) < ignoreChance) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onResistance(EffectReceiveEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getTarget() instanceof Player player)) return;
        if (event.getEffect().getApplier() != null && event.getEffect().getApplier().equals(player)) return;
        if (!event.getEffect().getEffectType().isNegative()) return;

        ItemStack[] armour = player.getInventory().getArmorContents();

        double reduction = 1.0 - (getSumOfRuneEffect(armour, RuneNamespacedKeys.RESISTANCE) / 100);
        event.getEffect().setLength((long) (event.getEffect().getRawLength() * reduction));

    }

    @EventHandler
    public void onResistanceVanilla(EntityPotionEffectEvent event) {
        if (event.isCancelled()) return;
        if (event.getNewEffect() == null) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) return;
        if (!UtilEffect.isNegativePotionEffect(event.getNewEffect())) return;

        ItemStack[] armour = player.getInventory().getArmorContents();
        UtilServer.runTaskLater(champions, () -> {
            player.removePotionEffect(event.getNewEffect().getType());
            double reduction = 1.0 - (getSumOfRuneEffect(armour, RuneNamespacedKeys.RESISTANCE) / 100);
            UtilEffect.applyCraftEffect(player, (new PotionEffect(event.getNewEffect().getType(), (int) (event.getNewEffect().getDuration() * reduction), event.getNewEffect().getAmplifier())));
        }, 1);

    }


    private Rune getRuneFromNamespacedKey(NamespacedKey key, ItemMeta itemMeta) {
        if (itemMeta == null) return null;

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        PersistentDataContainer runePdc = pdc.get(key, PersistentDataType.TAG_CONTAINER);
        if (runePdc == null) return null;

        String runeName = runePdc.get(RuneNamespacedKeys.OWNING_RUNE, PersistentDataType.STRING);
        if (runeName == null) return null;

        return (Rune) itemHandler.getItem(runeName);
    }

    private double getSumOfRuneEffect(ItemStack[] armour, NamespacedKey key) {
        double total = 0;
        for (ItemStack item : armour) {
            if (item == null) continue;
            ItemMeta itemMeta = item.getItemMeta();
            Rune rune = getRuneFromNamespacedKey(key, itemMeta);
            if (rune == null) continue;

            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
            if (pdc != null) {
                total += rune.getRollFromItem(pdc, rune.getAppliedNamespacedKey(), PersistentDataType.DOUBLE);
            }
        }
        return total;
    }
}
