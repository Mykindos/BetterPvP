package me.mykindos.betterpvp.core.combat.listeners;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.adapters.CustomDamageAdapter;
import me.mykindos.betterpvp.core.combat.armour.ArmourManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.data.DamageData;
import me.mykindos.betterpvp.core.combat.data.SoundProvider;
import me.mykindos.betterpvp.core.combat.events.CustomDamageDurabilityEvent;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.CustomDamageReductionEvent;
import me.mykindos.betterpvp.core.combat.events.CustomKnockbackEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.mykindos.betterpvp.core.utilities.UtilMessage.message;

@CustomLog
@BPvPListener
public class CombatListener implements Listener {

    private static final List<DamageCause> TRUE_DAMAGE_SOURCES = Arrays.asList(
            DamageCause.FIRE_TICK,
            DamageCause.FALL,
            DamageCause.LAVA,
            DamageCause.FIRE,
            DamageCause.DROWNING,
            DamageCause.SUFFOCATION,
            DamageCause.STARVATION,
            DamageCause.VOID,
            DamageCause.CONTACT,
            DamageCause.CRAMMING,
            DamageCause.HOT_FLOOR,
            DamageCause.FLY_INTO_WALL,
            DamageCause.KILL,
            DamageCause.MAGIC,
            DamageCause.WORLD_BORDER
    );

    private final List<DamageData> damageDataList;
    private final ClientManager clientManager;
    private final ArmourManager armourManager;
    private final DamageLogManager damageLogManager;
    private final EffectManager effectManager;
    private final List<CustomDamageAdapter> customDamageAdapters;

    @Inject
    public CombatListener(ClientManager clientManager, ArmourManager armourManager, DamageLogManager damageLogManager, EffectManager effectManager) {
        this.clientManager = clientManager;
        this.armourManager = armourManager;
        this.damageLogManager = damageLogManager;
        this.effectManager = effectManager;
        damageDataList = new ArrayList<>();
        customDamageAdapters = new ArrayList<>();

        boolean isMythicMobsEnabled = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
        try {
            if (isMythicMobsEnabled) {
                customDamageAdapters.add((CustomDamageAdapter) Class.forName("me.mykindos.betterpvp.core.combat.listeners.mythicmobs.MythicMobsAdapter").getDeclaredConstructor().newInstance());
            }
        } catch (Exception ex) {
            log.warn("Could not find MythicMobs plugin, adapter not loaded").submit();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void damageEvent(CustomDamageEvent event) {

        if (event.getForceDamageDelay() != 0 && event.isCancelled()) {
            String damagerUuid = event.getDamager() == null ? null : event.getDamager().getUniqueId().toString();
            damageDataList.add(new DamageData(event.getDamagee().getUniqueId().toString(), event.getCause(), damagerUuid, event.getForceDamageDelay()));
        }

        if (event.getDamagee().getHealth() <= 0) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        if (event.isDoVanillaEvent()) {
            return;
        }

        if (event.getDamagee() instanceof ArmorStand) {
            return;
        }

        if (event.getDamageDelay() > 0 && event.getDamager() != null) {
            effectManager.getEffect(event.getDamager(), EffectTypes.ATTACK_SPEED).ifPresent(effect -> {
                event.setDamageDelay((long) (event.getDamageDelay() * (1 - (effect.getAmplifier() / 100d))));
            });
            effectManager.getEffect(event.getDamager(), EffectTypes.CONCUSSED).ifPresent(effect -> {
                LivingEntity concussedPlayer = effect.getApplier();
                concussedPlayer.getWorld().playSound(concussedPlayer.getLocation(), Sound.ENTITY_GOAT_LONG_JUMP, 2.0F, 1.0F);

                event.setDamageDelay((long) (event.getDamageDelay() * (1 + (effect.getAmplifier() * 0.25))));
            });
        }

        damage(event);
    }

    private void damage(CustomDamageEvent event) {

        if (event.getDamagee().getHealth() > 0) {
            if (event.getDamage() >= 0) {

                String damagerUuid = event.getDamager() == null ? null : event.getDamager().getUniqueId().toString();

                if (event.getDamageDelay() > 0) {
                    damageDataList.add(new DamageData(event.getDamagee().getUniqueId().toString(), event.getCause(), damagerUuid, event.getDamageDelay()));
                }

                if (event.isKnockback() && event.getDamager() != null) {
                    CustomKnockbackEvent cke = UtilServer.callEvent(new CustomKnockbackEvent(event.getDamagee(), event.getDamager(), event.getDamage(), event));
                    if (!cke.isCancelled()) {
                        applyKB(cke);
                    }

                }

                CustomDamageReductionEvent customDamageReductionEvent = UtilServer.callEvent(new CustomDamageReductionEvent(event, event.getDamage()));
                customDamageReductionEvent.setDamage(armourManager.getDamageReduced(event.getDamage(), event.getDamagee()));

                event.setDamage(event.isIgnoreArmour() ? event.getDamage() : customDamageReductionEvent.getDamage());

                for (CustomDamageAdapter adapter : customDamageAdapters) {
                    if (!adapter.isValid(event)) {
                        continue;
                    }

                    if (adapter.processCustomDamageAdapter(event)) {
                        finalizeDamage(event, customDamageReductionEvent);
                        return;
                    }
                }

                playDamageEffect(event);
                finalizeDamage(event, customDamageReductionEvent);
            }
        }

    }

    private void finalizeDamage(CustomDamageEvent event, CustomDamageReductionEvent reductionEvent) {

        updateDurability(event);

        if (event.getProjectile() instanceof Arrow) {
            if (event.getDamager() instanceof Player player) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 0.7f);
                event.getDamager().getWorld().playSound(event.getDamagee().getLocation(), Sound.ENTITY_ARROW_HIT, 0.5f, 1.0f);
            }
        }

        if (!event.getDamagee().isDead()) {

            if (event.getDamagee() instanceof Player player) {
                if (player.getInventory().getItemInMainHand().getType() == Material.BOOK) {
                    final String modified = reductionEvent.getInitialDamage() == event.getRawDamage()
                            ? "<red>Unmodified" : "<orange>" + reductionEvent.getInitialDamage();
                    final String reduced = event.isIgnoreArmour() ? "<red>Disabled"
                            : reductionEvent.getInitialDamage() == reductionEvent.getDamage()
                            ? "<red>Unmodified" : "<orange>" + reductionEvent.getDamage();
                    final String knockback = event.isKnockback() ? "<green>Enabled" : "<red>Disabled";

                    player.sendMessage("");
                    message(player, "Combat", "Damage Breakdown:");
                    message(player, "Combat", "Initial Raw Damage: <orange>" + event.getRawDamage());
                    message(player, "Combat", "Modified Damage: " + modified);
                    message(player, "Combat", "Reduced Damage: " + reduced);
                    message(player, "Combat", "Knockback: " + knockback);
                    message(player, "Combat", "Delay: <#ededed>" + event.getDamageDelay());
                    message(player, "Combat", "Cause: <#ededed><i>" + event.getCause().name());
                    player.sendMessage("");
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

    private void updateDurability(CustomDamageEvent event) {

        CustomDamageDurabilityEvent cdde = new CustomDamageDurabilityEvent(event);
        if (!event.isDoDurability()) {
            cdde.setDamagerTakeDurability(false);
        }

        UtilServer.callEvent(cdde);

    }


    private void processDamageData(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player damagee) {
            clientManager.search().offline(damagee.getUniqueId(), client -> {
                if (client.isPresent()) {
                    final Gamer gamer = client.get().getGamer();
                    gamer.saveProperty(GamerProperty.DAMAGE_TAKEN, (double) gamer.getProperty(GamerProperty.DAMAGE_TAKEN).orElse(0D) + event.getDamage());
                    gamer.setLastDamaged(System.currentTimeMillis());
                }
            });
        }

        if (event.getDamager() instanceof Player damager) {
            clientManager.search().offline(damager.getUniqueId(), client -> {
                if (client.isPresent()) {
                    final Gamer gamer = client.get().getGamer();
                    gamer.setLastDamaged(System.currentTimeMillis());
                    gamer.saveProperty(GamerProperty.DAMAGE_DEALT, (double) gamer.getProperty(GamerProperty.DAMAGE_DEALT).orElse(0D) + event.getDamage());
                }
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

        if (hasDamageData(cde.getDamagee(), cde.getCause(), cde.getDamager())) {
            event.setCancelled(true);
            return;
        }

        if (cde.getCause() == DamageCause.ENTITY_ATTACK) {
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

        if (event.getCause() == DamageCause.POISON) {
            if (damagee.getHealth() < 2) {
                event.setCancelled(true);
            }
        }

        if (event.getCause() == DamageCause.LIGHTNING) {
            event.setCancelled(true);
        }

        if (event.getCause() == DamageCause.WITHER) {
            event.setCancelled(true);
        }

        if (event.isCancelled()) {
            return;
        }

        LivingEntity damager = getDamagerEntity(event);
        Entity damaging = getDamagingEntity(event);

        CustomDamageEvent cde = new CustomDamageEvent(damagee, damager, damaging, event.getCause(), event.getDamage(), true);
        UtilDamage.doCustomDamage(cde);

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handleCauseTimers(PreCustomDamageEvent event) {

        CustomDamageEvent cde = event.getCustomDamageEvent();
        if (cde.getDamageDelay() == 0) return;

        if (cde.getCause() == DamageCause.ENTITY_ATTACK
                || cde.getCause() == DamageCause.PROJECTILE
                || cde.getCause() == DamageCause.CUSTOM) {
            cde.setDamageDelay(400);
        }

        if (cde.getCause() == DamageCause.POISON) {
            cde.setDamageDelay(1000);
        }

        if (cde.getCause() == DamageCause.LAVA) {
            cde.setDamageDelay(400);
        }

        if (cde.getCause() == DamageCause.SUFFOCATION) {
            cde.setDamageDelay(400);
        }

        if (cde.getDamagee().getLocation().getBlock().isLiquid()) {
            if (cde.getCause() == DamageCause.FIRE || cde.getCause() == DamageCause.FIRE_TICK) {
                event.cancel("Already in lava / liquid");
            }
        }
    }

    public void applyKB(CustomKnockbackEvent event) {
        double knockback = event.getDamage();
        if (knockback < 2.0D && !event.isCanBypassMinimum()) knockback = 2.0D;

        knockback = Math.max(0, Math.log10(knockback));
        if (knockback == 0) return;

        Vector trajectory = UtilVelocity.getTrajectory2d(event.getDamager(), event.getDamagee());
        trajectory.multiply(0.6D * knockback);
        trajectory.setY(Math.abs(trajectory.getY()));

        if (event.getCustomDamageEvent().getProjectile() != null) {
            trajectory = event.getCustomDamageEvent().getProjectile().getVelocity();
            trajectory.setY(0);
            trajectory.multiply(0.37 * knockback / trajectory.length());
            trajectory.setY(0.06);
        }

        double strength = 0.2D + trajectory.length() * 0.8D;
        trajectory.multiply(event.getMultiplier());

        VelocityData velocityData = new VelocityData(trajectory, strength, false, 0.0D, Math.abs(0.2D * knockback), 0.4D + (0.04D * knockback), true);
        UtilVelocity.velocity(event.getDamagee(), event.getDamager(), velocityData, VelocityType.KNOCKBACK);
    }

    @UpdateEvent
    public void delayUpdater() {
        damageDataList.removeIf(damageData -> UtilTime.elapsed(damageData.getTimeOfDamage(), damageData.getDamageDelay()));
    }

    public boolean hasDamageData(LivingEntity damagee, DamageCause cause, @Nullable LivingEntity damager) {
        return damageDataList.stream().anyMatch(damageData -> {
            if (damageData.getUuid().equalsIgnoreCase(damagee.getUniqueId().toString())
                    && damageData.getCause() == cause) {
                if (damager == null || damageData.getDamager() == null) {
                    return true;
                } else {
                    return damageData.getDamager().equalsIgnoreCase(damager.getUniqueId().toString());
                }
            }

            return false;
        });
    }

    private Entity getDamagingEntity(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent ev)) {
            return null;
        }

        return ev.getDamager();
    }

    public static LivingEntity getDamagerEntity(EntityDamageEvent event) {

        if (!(event instanceof EntityDamageByEntityEvent ev)) {
            return null;
        }

        if ((ev.getDamager() instanceof LivingEntity damager)) {
            return damager;
        }

        if (ev.getDamager() instanceof TNTPrimed tnt && tnt.getSource() instanceof LivingEntity ent) {
            return ent;
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
        final LivingEntity damagee = event.getDamagee();
        if (event.isHurtAnimation()) {
            damagee.playHurtAnimation(270);
        }

        final SoundProvider provider = event.getSoundProvider();
        final net.kyori.adventure.sound.Sound sound = provider.apply(event);
        if (sound != null) {
            if (provider.fromEntity()) {
                damagee.getWorld().playSound(sound, damagee);
            } else {
                damagee.getWorld().playSound(damagee.getLocation(), sound.name().asString(), sound.volume(), sound.pitch());
            }
        }
    }

    @EventHandler
    public void onTrueDamage(CustomDamageEvent event) {
        if (TRUE_DAMAGE_SOURCES.contains(event.getCause())) {
            event.setIgnoreArmour(true);
        }
    }

    @EventHandler
    public void onCanHurt(EntityCanHurtEntityEvent event) {
        if (!event.isAllowed()) {
            return;
        }

        if (event.getDamagee().equals(event.getDamager())) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (event.getDamagee() instanceof Player damagee) {
            if (damagee.getGameMode() == GameMode.CREATIVE || damagee.getGameMode() == GameMode.SPECTATOR) {
                event.setResult(Event.Result.DENY);
            }
        }

    }

}
