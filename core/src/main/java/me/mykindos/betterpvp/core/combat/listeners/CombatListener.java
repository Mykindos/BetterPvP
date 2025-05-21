package me.mykindos.betterpvp.core.combat.listeners;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.adapters.CustomDamageAdapter;
import me.mykindos.betterpvp.core.combat.armour.ArmourManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.data.DamageData;
import me.mykindos.betterpvp.core.combat.data.FireData;
import me.mykindos.betterpvp.core.combat.data.SoundProvider;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.CustomDamageReductionEvent;
import me.mykindos.betterpvp.core.combat.events.CustomKnockbackEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.display.GamerDisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.experience.data.ExperienceLevelData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


import static me.mykindos.betterpvp.core.utilities.UtilMessage.message;

@SuppressWarnings("UnstableApiUsage")
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

    private final Core core;
    private final ClientManager clientManager;
    private final ArmourManager armourManager;
    private final DamageLogManager damageLogManager;
    private final EffectManager effectManager;
    private final List<CustomDamageAdapter> customDamageAdapters;

    private final List<DamageData> damageDataList;
    private final WeakHashMap<LivingEntity, FireData> fireDamageSource;
    private final Set<UUID> delayKillSet = new HashSet<>();

    private final GamerDisplayObject<ExperienceLevelData> gamerDisplayObject;

    @Inject
    public CombatListener(Core core, ClientManager clientManager, ArmourManager armourManager, DamageLogManager damageLogManager, EffectManager effectManager) {
        this.core = core;
        this.clientManager = clientManager;
        this.armourManager = armourManager;
        this.damageLogManager = damageLogManager;
        this.effectManager = effectManager;
        damageDataList = new ArrayList<>();
        customDamageAdapters = new ArrayList<>();
        fireDamageSource = new WeakHashMap<>();

        this.gamerDisplayObject = new GamerDisplayObject<>((gamer) -> new ExperienceLevelData((int) gamer.getLastDealtDamageValue()));


        initializeAdapters();
    }

    private void initializeAdapters() {
        boolean isMythicMobsEnabled = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
        try {
            if (isMythicMobsEnabled) {
                customDamageAdapters.add((CustomDamageAdapter) Class.forName(
                                "me.mykindos.betterpvp.core.combat.listeners.mythicmobs.MythicMobsAdapter")
                        .getDeclaredConstructor().newInstance());
            }
        } catch (Exception ex) {
            log.warn("Could not find MythicMobs plugin, adapter not loaded").submit();
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void damageEvent(DamageEvent event) {
        if (shouldSkipDamageEvent(event)) {
            return;
        }

        applyAttackSpeedEffects(event);
        damage(event);
    }

    private boolean shouldSkipDamageEvent(DamageEvent event) {
        if (event.getForceDamageDelay() != 0 && event.isCancelled()) {
            String damagerUuid = event.getDamager() == null ? null : event.getDamager().getUniqueId().toString();
            damageDataList.add(new DamageData(event.getDamagee().getUniqueId().toString(),
                    event.getCause(), damagerUuid, event.getForceDamageDelay()));
            return true;
        }

        if (event instanceof CustomDamageEvent cde && cde.getDamagee().getHealth() <= 0) {
            return true;
        }

        if (event.isCancelled() || event.isDoVanillaEvent() || event.getDamagee() instanceof ArmorStand) {
            return true;
        }

        return false;
    }

    private void applyAttackSpeedEffects(DamageEvent event) {
        if (event.getDamageDelay() <= 0 || event.getDamager() == null) {
            return;
        }

        effectManager.getEffect(event.getDamager(), EffectTypes.ATTACK_SPEED).ifPresent(effect -> {
            event.setDamageDelay((long) (event.getDamageDelay() * (1 - (effect.getAmplifier() / 100d))));
        });

        effectManager.getEffect(event.getDamager(), EffectTypes.CONCUSSED).ifPresent(effect -> {
            LivingEntity concussedPlayer = event.getDamager();
            concussedPlayer.getWorld().playSound(concussedPlayer.getLocation(), Sound.ENTITY_GOAT_LONG_JUMP, 2.0F, 1.0F);
            event.setDamageDelay((long) (event.getDamageDelay() * (1 + (effect.getAmplifier() * 0.25))));
        });
    }


    private void damage(DamageEvent event) {
        if (event.getDamage() < 0 || (event instanceof CustomDamageEvent cde && cde.getDamagee().getHealth() <= 0)) {
            return;
        }

        // Process all damage modifiers before applying final damage
        event.processDamageModifiers();
        addDamageData(event);

        CustomDamageReductionEvent reductionEvent = null;
        if (event instanceof CustomDamageEvent cde) {
            processCustomDamageEvent(cde);
            reductionEvent = calculateDamageReduction(cde);

            for (CustomDamageAdapter adapter : customDamageAdapters) {
                if (adapter.isValid(cde) && adapter.processCustomDamageAdapter(cde)) {
                    finalizeDamage(cde, reductionEvent);
                    return;
                }
            }

            playDamageEffect(cde, reductionEvent);
        }

        finalizeDamage(event, reductionEvent);

    }

    private void addDamageData(DamageEvent event) {
        if (event.getDamageDelay() > 0) {
            String damagerUuid = event.getDamager() == null ? null : event.getDamager().getUniqueId().toString();
            damageDataList.add(new DamageData(event.getDamagee().getUniqueId().toString(),
                    event.getCause(), damagerUuid, event.getDamageDelay()));
        }
    }

    private void processCustomDamageEvent(CustomDamageEvent cde) {
        if (cde.isKnockback() && cde.getDamager() != null) {
            CustomKnockbackEvent cke = UtilServer.callEvent(new CustomKnockbackEvent(
                    cde.getDamagee(), cde.getDamager(), cde.getDamage(), cde));
            if (!cke.isCancelled()) {
                applyKB(cke);
            }
        }
    }

    private CustomDamageReductionEvent calculateDamageReduction(CustomDamageEvent cde) {
        CustomDamageReductionEvent reductionEvent = UtilServer.callEvent(
                new CustomDamageReductionEvent(cde, cde.getDamage()));

        double reducedDamage = cde.isIgnoreArmour() ?
                cde.getDamage() :
                armourManager.getDamageReduced(cde.getDamage(), cde.getDamagee());

        reductionEvent.setDamage(reducedDamage);
        cde.setDamage(reducedDamage);

        return reductionEvent;
    }


    private void finalizeDamage(DamageEvent event, CustomDamageReductionEvent reductionEvent) {
        if (event instanceof CustomDamageEvent cde) {
            updateDurability(cde);
        }

        playHitSounds(event);

        if (event.getDamagee().isDead() || !(event instanceof CustomDamageEvent cde)) {
            return;
        }

        displayDebugInfo(event, reductionEvent);
        processDamageData(event);

        applyFinalDamage(cde);
    }

    private void playHitSounds(DamageEvent event) {
        final DamageSource source = event.getDamageSource();
        if (source.isIndirect() && source.getCausingEntity() instanceof Player player
                && source.getDirectEntity() instanceof Arrow) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 0.7f);
            event.getDamager().getWorld().playSound(event.getDamagee().getLocation(),
                    Sound.ENTITY_ARROW_HIT, 0.5f, 1.0f);
        }
    }

    private void displayDebugInfo(DamageEvent event, CustomDamageReductionEvent reductionEvent) {
        if (!(event.getDamagee() instanceof Player player) ||
                player.getInventory().getItemInMainHand().getType() != Material.BOOK ||
                !(event instanceof CustomDamageEvent cde)) {
            return;
        }

        final String modified = reductionEvent.getInitialDamage() == event.getRawDamage()
                ? "<red>Unmodified" : "<orange>" + reductionEvent.getInitialDamage();

        final String reduced = cde.isIgnoreArmour() ? "<red>Disabled"
                : reductionEvent.getInitialDamage() == reductionEvent.getDamage()
                ? "<red>Unmodified" : "<orange>" + reductionEvent.getDamage();

        final String knockback = cde.isKnockback() ? "<green>Enabled" : "<red>Disabled";

        player.sendMessage("");
        message(player, "Combat", "Health: <red>" + player.getHealth());
        message(player, "Combat", "Damage Breakdown:");
        message(player, "Combat", "Initial Raw Damage: <orange>" + event.getRawDamage());
        message(player, "Combat", "Modified Damage: " + modified);
        message(player, "Combat", "Reduced Damage: " + reduced);
        message(player, "Combat", "Knockback: " + knockback);
        message(player, "Combat", "Delay: <#ededed>" + event.getDamageDelay());
        message(player, "Combat", "Cause: <#ededed><i>" + event.getCause().name());
        player.sendMessage("");
    }

    private void applyFinalDamage(CustomDamageEvent cde) {
        if (cde.getDamagee().getHealth() - cde.getDamage() < 1.0) {
            // Temporary measure to fix https://github.com/PaperMC/Paper/issues/12148
            if (!delayKillSet.contains(cde.getDamagee().getUniqueId())) {
                delayKillSet.add(cde.getDamagee().getUniqueId());
                UtilServer.runTaskLater(core, () -> {
                    cde.getDamagee().setHealth(0);
                    delayKillSet.remove(cde.getDamagee().getUniqueId());
                }, 1L);
            }
        } else {
            cde.getDamagee().setHealth(cde.getDamagee().getHealth() - cde.getDamage());
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onDeadCantDealDamage(DamageEvent event) {
        if (event.getDamager() == null) return;
        if (delayKillSet.contains(event.getDamager().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void updateDurability(CustomDamageEvent event) {
        if (event.isDoDurability() && event.getDamager() instanceof Player damager) {
            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                UtilItem.damageItem(damager, damager.getInventory().getItemInMainHand(), 1);
            }
        }
        if (event.getDamagee() instanceof Player damagee) {
            for (ItemStack armour : damagee.getEquipment().getArmorContents()) {
                if (armour == null) continue;
                UtilItem.damageItem(damagee, armour, 1);
            }
        }
    }


    private void processDamageData(DamageEvent event) {
        processPlayerDamageTaken(event);
        processPlayerDamageDealt(event);
        recordDamageLog(event);

    }

    private void processPlayerDamageTaken(DamageEvent event) {
        if (event.getDamagee() instanceof Player damagee) {
            clientManager.search().offline(damagee.getUniqueId()).thenAcceptAsync(client -> {
                if (client.isPresent()) {
                    final Gamer gamer = client.get().getGamer();
                    gamer.saveProperty(GamerProperty.DAMAGE_TAKEN,
                            (double) gamer.getProperty(GamerProperty.DAMAGE_TAKEN).orElse(0D) + event.getDamage());

                    if (event.getCause() != DamageCause.FALL) {
                        gamer.setLastDamaged(System.currentTimeMillis());
                    }
                }
            });
        }
    }

    private void processPlayerDamageDealt(DamageEvent event) {
        if (event.getDamager() instanceof Player damager) {
            clientManager.search().offline(damager.getUniqueId()).thenAcceptAsync(client -> {
                if (client.isPresent()) {
                    final Gamer gamer = client.get().getGamer();
                    gamer.setLastDamaged(System.currentTimeMillis());
                    gamer.saveProperty(GamerProperty.DAMAGE_DEALT,
                            (double) gamer.getProperty(GamerProperty.DAMAGE_DEALT).orElse(0D) + event.getDamage());
                    gamer.setLastDealtDamageValue(event.getDamage());
                }
            });
        }
    }

    private void recordDamageLog(DamageEvent event) {
        DamageLog damageLog = new DamageLog(event.getDamager(), event.getCause(),
                event.getDamage(), event.getReason());
        damageLogManager.add(event.getDamagee(), damageLog);
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreDamage(PreDamageEvent event) {
        DamageEvent de = event.getDamageEvent();

        if (de instanceof CustomDamageEvent cde) {
            for (CustomDamageAdapter adapter : customDamageAdapters) {
                if (!adapter.isValid(cde)) {

                    continue;
                }

                if (!adapter.processPreCustomDamage(cde)) {
                    event.setCancelled(true);
                    return;
                }
                break;
            }
        }

        if (de.getDamagee().equals(de.getDamager())) {
            event.setCancelled(true);
            return;
        }

        if (UtilPlayer.isCreativeOrSpectator(de.getDamagee())) {
            event.setCancelled(true);
            return;
        }

        if (hasDamageData(de.getDamagee(), de.getCause(), de.getDamager())) {
            event.setCancelled(true);
            return;
        }

        if (de.getCause() == DamageCause.ENTITY_ATTACK && de.getDamager() != null && de.getDamager().getHealth() <= 0) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void startCustomDamageEvent(EntityDamageEvent event) {
        if (event.isCancelled()) {
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

        if (event.getCause() == DamageCause.LIGHTNING) {
            event.setCancelled(true);
        }

        if (event.getCause() == DamageCause.WITHER) {
            event.setCancelled(true);
        }

        if (event.isCancelled()) {
            return;
        }

        DamageSource source = event.getDamageSource();
        boolean knockback = true;
        if (source.getDirectEntity() instanceof TNTPrimed tnt && tnt.getSource() != null) {
            source = DamageSource.builder(DamageType.PLAYER_EXPLOSION)
                    .withDirectEntity(tnt)
                    .withCausingEntity(tnt.getSource())
                    .withDamageLocation(source.getDirectEntity().getLocation())
                    .build();
        }

        if (event.getEntity() instanceof LivingEntity damagee) {
            if (event.getCause() == DamageCause.POISON) {
                if (damagee.getHealth() < 2) {
                    event.setCancelled(true);
                }
            }
            if (event.getCause() == DamageCause.FIRE_TICK) {
                knockback = false;
                if (fireDamageSource.containsKey(damagee)) {
                    FireData fireData = fireDamageSource.get(damagee);
                    source = DamageSource.builder(DamageType.ON_FIRE)
                            .withDirectEntity(fireData.getDamager())
                            .withCausingEntity(fireData.getDamager())
                            .build();
                }
            }

            CustomDamageEvent cde = new CustomDamageEvent(damagee, source, event.getCause(), event.getDamage(), knockback);
            UtilDamage.doCustomDamage(cde);
        } else {
            DamageEvent de = new DamageEvent(event.getEntity(), source, event.getCause(), event.getDamage());
            UtilDamage.doDamage(de);
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(PreDamageEvent event) {
        if (event.getDamageEvent().getDamagingEntity() == null) {
            return;
        }

        final Boolean key = event.getDamageEvent().getDamagingEntity().getPersistentDataContainer().get(CoreNamespaceKeys.NO_DAMAGE, PersistentDataType.BOOLEAN);
        if (key != null && key) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handleCauseTimers(PreDamageEvent event) {

        DamageEvent cde = event.getDamageEvent();
        if (cde.getDamageDelay() == 0) return;

        if (cde.getCause() == DamageCause.ENTITY_ATTACK
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

        double strength = 0.2D + trajectory.length() * 0.9D;
        trajectory.multiply(event.getMultiplier());

        VelocityData velocityData = new VelocityData(trajectory, strength, false, 0.0D, Math.abs(0.2D * knockback), 0.4D + (0.04D * knockback), true);
        UtilVelocity.velocity(event.getDamagee(), event.getDamager(), velocityData, VelocityType.KNOCKBACK);
    }

    @UpdateEvent
    public void delayUpdater() {
        damageDataList.removeIf(damageData -> UtilTime.elapsed(damageData.getTimeOfDamage(), damageData.getDamageDelay()));
        fireDamageSource.forEach((livingEntity, fireData) -> {
            if (UtilTime.elapsed(fireData.getStart(), fireData.getDuration() + 50L)) {
                UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
                    fireDamageSource.remove(livingEntity);
                }, 1L);
            }
        });
    }

    public boolean hasDamageData(Entity damagee, DamageCause cause, @Nullable Entity damager) {
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

    private void playDamageEffect(CustomDamageEvent event, CustomDamageReductionEvent damageReductionEvent) {
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityCombustByEntity(EntityCombustByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (!(event.getCombuster() instanceof LivingEntity combusterEntity)) return;
        this.fireDamageSource.put(livingEntity,
                new FireData(combusterEntity,
                        (long) (event.getDuration() * 20L * 1000L))
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onClientLogin(ClientJoinEvent event) {
        event.getClient().getGamer().getExperienceLevel().add(500, gamerDisplayObject);
    }
}
