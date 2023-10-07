package me.mykindos.betterpvp.core.combat.listeners;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.combat.adapters.CustomDamageAdapter;
import me.mykindos.betterpvp.core.combat.armour.ArmourManager;
import me.mykindos.betterpvp.core.combat.data.DamageData;
import me.mykindos.betterpvp.core.combat.events.*;
import me.mykindos.betterpvp.core.combat.log.DamageLog;
import me.mykindos.betterpvp.core.combat.log.DamageLogManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@BPvPListener
public class CombatListener implements Listener {

    private final List<DamageData> damageDataList;
    private final GamerManager gamerManager;
    private final ArmourManager armourManager;
    private final DamageLogManager damageLogManager;

    private final List<CustomDamageAdapter> customDamageAdapters;

    @Inject
    public CombatListener(GamerManager gamerManager, ArmourManager armourManager, DamageLogManager damageLogManager) {
        this.gamerManager = gamerManager;
        this.armourManager = armourManager;
        this.damageLogManager = damageLogManager;
        damageDataList = new ArrayList<>();
        customDamageAdapters = new ArrayList<>();

        boolean isMythicMobsEnabled = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
        try {
            if (isMythicMobsEnabled) {
                customDamageAdapters.add((CustomDamageAdapter) Class.forName("me.mykindos.betterpvp.core.combat.listeners.mythicmobs.MythicMobsAdapter").getDeclaredConstructor().newInstance());
            }
        } catch (Exception ex) {
            log.warn("Could not find MythicMobs plugin, adapter not loaded");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void damageEvent(CustomDamageEvent event) {

        if (event.isCancelled()) {
            return;
        }

        if (event.isDoVanillaEvent()) {
            return;
        }

        // TODO cancel this elsewhere...
        if (event.getDamagee() instanceof ArmorStand) {
            return;
        }

        damage(event);
    }

    private void damage(CustomDamageEvent event) {

        if (event.getDamagee().getHealth() > 0) {
            if (event.getDamage() >= 0) {

                damageDataList.add(new DamageData(event.getDamagee().getUniqueId().toString(), event.getCause(), event.getDamageDelay()));

                if (event.isKnockback()) {
                    if (event.getDamager() != null) {
                        CustomKnockbackEvent cke = UtilServer.callEvent(new CustomKnockbackEvent(event.getDamagee(), event.getDamager(), event.getDamage(), event));
                        if (!cke.isCancelled()) {
                            applyKB(cke);
                        }
                    }
                }

                CustomDamageReductionEvent customDamageReductionEvent = UtilServer.callEvent(new CustomDamageReductionEvent(event, event.getDamage()));
                customDamageReductionEvent.setDamage(armourManager.getDamageReduced(event.getDamage(), event.getDamagee()));

                event.setRawDamage(event.getDamage());
                event.setDamage(event.isIgnoreArmour() ? event.getDamage() : customDamageReductionEvent.getDamage());

                for (CustomDamageAdapter adapter : customDamageAdapters) {
                    if (!adapter.isValid(event)) {
                        continue;
                    }

                    adapter.processCustomDamageAdapter(event);
                    finalizeDamage(event);
                    return;
                }

                playDamageEffect(event);
                finalizeDamage(event);
            }
        }

    }

    private void finalizeDamage(CustomDamageEvent event) {
        updateDurability(event);

        if (!event.getDamagee().isDead()) {

            if (event.getDamagee() instanceof Player player) {
                if (player.getInventory().getItemInMainHand().getType() == Material.BOOK) {
                    player.sendMessage("");
                    player.sendMessage("Initial Damage: " + event.getRawDamage());
                    player.sendMessage("Damage after reduction: " + event.getDamage());
                    player.sendMessage("Delay: " + event.getDamageDelay());
                    player.sendMessage("Cause: " + event.getCause().name());

                }
            }

            processDamageData(event);

            if (event.getDamagee().getHealth() - event.getDamage() < 1.0) {
                event.getDamagee().setHealth(0);
            } else {
                event.getDamagee().setHealth(event.getDamagee().getHealth() - event.getDamage());
            }

        }
    }

    private void processDamageData(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player damagee) {
            gamerManager.getObject(damagee.getUniqueId()).ifPresent(gamer -> {
                gamer.setLastDamaged(System.currentTimeMillis());
                gamer.saveProperty(GamerProperty.DAMAGE_TAKEN, event.getDamage());
            });
        }

        if (event.getDamager() instanceof Player damager) {
            gamerManager.getObject(damager.getUniqueId()).ifPresent(gamer -> {
                gamer.saveProperty(GamerProperty.DAMAGE_DEALT, event.getDamage());
            });
        }

        DamageLog damageLog = new DamageLog(event.getDamager(), event.getCause(), event.getDamage(), event.getReason());
        damageLogManager.add(event.getDamagee(), damageLog);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreDamage(PreCustomDamageEvent event) {
        CustomDamageEvent cde = event.getCustomDamageEvent();

        for (CustomDamageAdapter adapter : customDamageAdapters) {
            if (!adapter.isValid(event.getCustomDamageEvent())) {

                continue;
            }

            if (!adapter.processPreCustomDamage(event.getCustomDamageEvent())) {
                event.setCancelled(true);
                return;
            }
            break;
        }

        if (cde.getDamager() != null) {
            if (cde.getDamager().equals(cde.getDamagee())) {
                event.setCancelled(true);
                return;
            }
        }

        if (UtilPlayer.isCreativeOrSpectator(cde.getDamagee())) {
            event.setCancelled(true);
            return;
        }

        if (hasDamageData(cde.getDamagee().getUniqueId().toString(), cde.getCause())) {
            event.setCancelled(true);
            return;
        }

        if (cde.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            if (cde.getDamager() != null) {
                if (cde.getDamager().getHealth() <= 0) {
                    event.setCancelled(true);
                }
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void startCustomDamageEvent(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }


        if (!(event.getEntity() instanceof LivingEntity damagee)) {
            return;
        }


        if ((event instanceof EntityDamageByEntityEvent ev)) {

            if (ev.getDamager() instanceof EvokerFangs) {
                event.setCancelled(true);
            }

            if (ev.getDamager() instanceof FishHook fishHook) {
                if (fishHook.getShooter() instanceof Player) {
                    return;
                }

            }
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.POISON) {
            if (damagee.getHealth() < 2) {
                event.setCancelled(true);
            }
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
            event.setCancelled(true);
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            event.setCancelled(true);
        }


        // TODO move elsewhere

        //if (event.getEntity() instanceof Sheep sheep) {
        //    if (sheep.customName() != null) {
        //        event.setCancelled(true);
        //    }
        //}

        if (event.isCancelled()) {
            return;
        }

        LivingEntity damager = getDamagerEntity(event);
        Projectile proj = getProjectile(event);

        CustomDamageEvent cde = new CustomDamageEvent(damagee, damager, proj, event.getCause(), event.getDamage(), true);
        UtilDamage.doCustomDamage(cde);

        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handleCauseTimers(CustomDamageEvent e) {

        if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || e.getCause() == EntityDamageEvent.DamageCause.PROJECTILE
                || e.getCause() == EntityDamageEvent.DamageCause.CUSTOM) {
            e.setDamageDelay(400);
        }

        if (e.getCause() == EntityDamageEvent.DamageCause.POISON) {
            e.setDamageDelay(1000);
        }

        if (e.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            e.setDamageDelay(400);
        }

        if (e.getDamagee().getLocation().getBlock().isLiquid()) {
            if (e.getCause() == EntityDamageEvent.DamageCause.FIRE || e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                e.cancel("Already in lava / liquid");
            }
        }


    }

    @EventHandler
    public void onKnockbackSprintBonus(CustomKnockbackEvent event) {
        double knockback = event.getDamage();
        if (event.getDamager() instanceof Player player) {
            if (player.isSprinting()) {
                if (event.getCustomDamageEvent().getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                    knockback += 3;
                }
            }
        }

        event.setDamage(knockback);
    }

    public void applyKB(CustomKnockbackEvent event) {
        double knockback = event.getDamage();
        if (knockback < 2.0D && !event.isCanBypassMinimum()) knockback = 2.0D;
        knockback = Math.log10(knockback);

        Vector trajectory = UtilVelocity.getTrajectory2d(event.getDamager(), event.getDamagee());
        trajectory.multiply(0.6D * knockback);
        trajectory.setY(Math.abs(trajectory.getY()));

        if (event.getCustomDamageEvent().getProjectile() != null) {
            trajectory = event.getCustomDamageEvent().getProjectile().getVelocity();
            trajectory.setY(0);
            trajectory.multiply(0.37 * knockback / trajectory.length());
            trajectory.setY(0.06);
        }

        double velocity = 0.2D + trajectory.length() * 0.8D;

        UtilVelocity.velocity(event.getDamagee(),
                trajectory, velocity, false, 0.0D, Math.abs(0.2D * knockback), 0.4D + (0.04D * knockback), true);
    }

    @UpdateEvent
    public void delayUpdater() {
        damageDataList.removeIf(damageData -> UtilTime.elapsed(damageData.getTimeOfDamage(), damageData.getDamageDelay()));
    }

    public boolean hasDamageData(String uuid, EntityDamageEvent.DamageCause cause) {
        return damageDataList.stream().anyMatch(damageData -> damageData.getUuid().equalsIgnoreCase(uuid) && damageData.getCause() == cause);
    }

    private Projectile getProjectile(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent ev)) {
            return null;
        }

        if ((ev.getDamager() instanceof Projectile)) {
            return (Projectile) ev.getDamager();
        }
        return null;
    }

    public static LivingEntity getDamagerEntity(EntityDamageEvent event) {

        if (!(event instanceof EntityDamageByEntityEvent ev)) {
            return null;
        }

        if ((ev.getDamager() instanceof LivingEntity)) {
            return (LivingEntity) ev.getDamager();
        }

        if (!(ev.getDamager() instanceof Projectile projectile)) {
            return null;
        }

        if (projectile.getShooter() == null) {
            return null;
        }
        if (!(projectile.getShooter() instanceof LivingEntity)) {
            return null;
        }
        return (LivingEntity) projectile.getShooter();
    }

    private void playDamageEffect(CustomDamageEvent event) {
        event.getDamagee().playHurtAnimation(0);
        if (event.getProjectile() instanceof Arrow) {
            if (event.getDamager() instanceof Player player) {

                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 0.7f);
                event.getDamager().getWorld().playSound(event.getDamagee().getLocation(), Sound.ENTITY_ARROW_HIT, 0.5f, 1.0f);

            }
        } else {

            event.getDamagee().getWorld().playSound(event.getDamagee().getLocation(),
                    getDamageSound(event.getDamagee().getType()), 1.0F, 1.0F);

        }
    }

    public Sound getDamageSound(EntityType entityType) {
        try {
            String entName = entityType.name().toUpperCase();
            return Sound.valueOf("ENTITY_" + entName + "_HURT");
        } catch (IllegalArgumentException ignore) {
        }

        return Sound.ENTITY_PLAYER_HURT;
    }

    private void updateDurability(CustomDamageEvent event) {

        CustomDamageDurabilityEvent durabilityEvent = UtilServer.callEvent(new CustomDamageDurabilityEvent(event));

        if (durabilityEvent.isDamageeTakeDurability()) {
            if (event.getDamagee() instanceof Player damagee) {

                for (ItemStack armour : damagee.getEquipment().getArmorContents()) {
                    if (armour == null) continue;
                    ItemMeta meta = armour.getItemMeta();
                    if (meta instanceof Damageable armourMeta) {
                        armourMeta.setDamage(armourMeta.getDamage() + 1);
                        armour.setItemMeta(armourMeta);

                        if (armourMeta.getDamage() > armour.getType().getMaxDurability()) {
                            if (armour.getType().name().contains("HELMET")) {
                                damagee.getEquipment().setHelmet(null);
                            }
                            if (armour.getType().name().contains("CHESTPLATE")) {
                                damagee.getEquipment().setChestplate(null);
                            }
                            if (armour.getType().name().contains("LEGGINGS")) {
                                damagee.getEquipment().setLeggings(null);
                            }
                            if (armour.getType().name().contains("BOOTS")) {
                                damagee.getEquipment().setBoots(null);
                            }

                            damagee.playSound(damagee.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
                        }
                    }

                }

                damagee.updateInventory();
            }
        }

        if (durabilityEvent.isDamagerTakeDurability()) {
            if (event.getDamager() instanceof Player damager) {
                if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;


                ItemStack weapon = damager.getInventory().getItemInMainHand();
                if (weapon.getType() == Material.AIR) return;
                if (weapon.getType().getMaxDurability() == 0) return;

                ItemMeta meta = weapon.getItemMeta();
                if (meta instanceof Damageable weaponMeta) {
                    weaponMeta.setDamage(weaponMeta.getDamage() + 1);
                    weapon.setItemMeta(weaponMeta);

                    if (weaponMeta.getDamage() > weapon.getType().getMaxDurability()) {
                        damager.getInventory().setItemInMainHand(null);
                        damager.playSound(damager.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
                    }

                    damager.updateInventory();
                }


            }
        }

    }

    @EventHandler
    public void onFireDamage(CustomDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            event.setIgnoreArmour(true);
        }
    }

    /**
     * Disable bow critical hits
     *
     * @param event The event
     */
    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getProjectile() instanceof Arrow arrow) {
            arrow.setCritical(false);
        }
    }


}
