package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerCanUseSkillEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.actionbar.ActionBar;
import me.mykindos.betterpvp.core.utilities.model.display.component.TimedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@BPvPListener
public class Meditation extends Skill implements CooldownToggleSkill, DefensiveSkill, HealthSkill, Listener {
    private final Map<Player, Channel> channels = new HashMap<>();
    private final Map<Player, Protection> protection = new HashMap<>();
    private final Map<Player, PendingBurst> pendingBursts = new HashMap<>();
    private long duration;
    private long interval;
    private long finalPhase;
    private long cancelDelay;
    private long exitDuration;
    private double healing;
    private double finalHealing;
    private double exitReduction;
    private double burstReflectRatio; // fraction of blocked damage released on exit
    private double burstMaxDamage;    // hard cap on the burst regardless of how much was blocked
    private double burstRadius;
    private double burstKnockback;
    private long burstTelegraph;      // wind-up between leaving Meditation and the burst going off

    @Inject
    public Meditation(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() { return "Meditation"; }

    @Override
    public Role getClassType() { return Role.ASSASSIN; }

    @Override
    public SkillType getType() { return SkillType.PASSIVE_A; }

    @Override
    public double getCooldown(int level) { return cooldown; }

    @Override
    public boolean isDelayedSkill() { return true; }

    @Override
    public boolean isUsingSkill(Player player) { return channels.containsKey(player); }

    @Override
    public boolean canReactivate(Player player) {
        Channel channel = channels.get(player);
        return channel != null && channel.timeline.canCancel(now());
    }

    @Override
    public boolean displayWhenUsed() { return false; }

    @Override
    public boolean canToggleWith(ItemStack item) {
        SkillType type = SkillWeapons.getTypeFrom(item);
        return type == SkillType.SWORD || type == SkillType.AXE;
    }

    @Override
    public Component[] getDescription(int level) {
        return Translations.componentLines("champions.skill.assassin.meditation.description",
                getValueComponent(ignored -> duration / 1000.0, level),          // {0}
                getValueComponent(ignored -> timeline(0).totalHealing(), level), // {1}
                getValueComponent(ignored -> cancelDelay / 1000.0, level),       // {2}
                getValueComponent(ignored -> exitReduction * 100, level),        // {3}
                getValueComponent(ignored -> exitDuration / 1000.0, level),      // {4}
                getValueComponent(this::getCooldown, level),                     // {5}
                getValueComponent(ignored -> finalPhase / 1000.0, level),        // {6}
                getValueComponent(ignored -> burstMaxDamage, level),             // {7}
                getValueComponent(ignored -> burstRadius, level));               // {8}
    }

    @Override
    public boolean canUse(Player player) {
        if (!isEnabled() || getLevel(player) <= 0 || !player.isOnline() || player.isDead()) return false;
        if (channels.containsKey(player)) return true;
        // Also prevents the delayed-cooldown updater from restarting a cooldown already committed by end().
        if (championsManager.getCooldowns().hasCooldown(player, getName())) return false;
        if (UtilPlayer.isCreativeOrSpectator(player) || player.isInsideVehicle() || player.isGliding()
                || player.isFlying() || player.isSwimming() || UtilBlock.isInLiquid(player)
                || player.getVelocity().getY() > 0.05 || !hasGround(player)) {
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.assassin.meditation.ground");
            return false;
        }
        if (player.isInvisible() || player.hasPotionEffect(PotionEffectType.INVISIBILITY)
                || championsManager.getEffects().hasEffect(player, EffectTypes.VANISH)) {
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.assassin.meditation.visible");
            return false;
        }
        return true;
    }

    private boolean hasGround(Player player) {
        BoundingBox body = player.getBoundingBox();
        BoundingBox feet = new BoundingBox(body.getMinX(), body.getMinY() - 0.06, body.getMinZ(),
                body.getMaxX(), body.getMinY(), body.getMaxZ());
        int y = (int) Math.floor(feet.getMinY());
        for (int x = (int) Math.floor(feet.getMinX()); x <= Math.floor(feet.getMaxX()); x++) {
            for (int z = (int) Math.floor(feet.getMinZ()); z <= Math.floor(feet.getMaxZ()); z++) {
                if (!player.getWorld().isChunkLoaded(x >> 4, z >> 4)) continue;
                if (UtilBlock.getBoundingBoxes(player.getWorld().getBlockAt(x, y, z)).stream().anyMatch(feet::overlaps)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void toggle(Player player, int level) {
        Channel existing = channels.get(player);
        if (existing != null) {
            if (existing.timeline.canCancel(now())) end(player, true);
            return;
        }
        long now = now();
        ActionBar bar = championsManager.getClientManager().search().online(player).getGamer().getActionBar();
        Channel channel = new Channel(player.getLocation().clone(), timeline(now), bar,
                getCooldown(level), exitDuration, exitReduction);
        // Time bar rendered like a live cooldown; a red charge bar trails it once blows start landing.
        channel.display = new TimedComponent(duration / 1000.0 + 1, false, viewer -> {
            if (channels.get(player) != channel) return null;
            double total = channel.timeline.duration / 1000.0;
            double remaining = Math.max(0, channel.timeline.duration - (now() - now)) / 1000.0;
            if (remaining <= 0) return null;
            float progress = (float) Math.max(0, Math.min(1, 1 - remaining / total));
            Component line = Component.join(JoinConfiguration.separator(Component.space()),
                    getDisplayName().color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD),
                    ProgressBar.withProgress(progress).build(),
                    Component.text(String.format(java.util.Locale.ROOT, "%.1fs", remaining), NamedTextColor.WHITE));
            double burst = burstDamage(channel.charge);
            if (burst > 0) {
                line = line.append(Component.text("   ☠ ", NamedTextColor.DARK_GRAY))
                        .append(ProgressBar.withLength((float) dangerFraction(channel), 8)
                                .withProgressColor(NamedTextColor.RED)
                                .withRemainingColor(NamedTextColor.DARK_GRAY).build())
                        .append(Component.text(String.format(java.util.Locale.ROOT, " %.0f", burst), NamedTextColor.RED));
            }
            return line;
        });
        channels.put(player, channel);
        protection.remove(player);
        bar.add(getPriority() - 1, channel.display);
        player.closeInventory();
        if (channels.get(player) != channel) return; // inventory-close listeners can teleport or dequip
        player.clearActiveItem();
        player.setVelocity(new Vector());
        playActivation(player);
        particles(channel.anchor, channel.timeline.started, false, 0);
    }

    private MeditationTimeline timeline(long now) {
        return new MeditationTimeline(now, duration, interval, finalPhase, cancelDelay, healing, finalHealing);
    }

    @UpdateEvent
    public void update() {
        long now = now();
        for (Player player : List.copyOf(channels.keySet())) {
            Channel channel = channels.get(player);
            if (channel == null) continue;
            if (!isEnabled() || getLevel(player) <= 0 || !player.isOnline() || player.isDead()
                    || !player.getWorld().equals(channel.anchor.getWorld()) || UtilPlayer.isCreativeOrSpectator(player)) {
                clear(player);
                continue;
            }
            player.setVelocity(new Vector());
            double danger = dangerFraction(channel);
            // Consume each scheduled heal once, including the tick at exactly the channel's endpoint.
            while (channels.get(player) == channel && channel.timeline.hasDueHeal(now)) {
                double amount = channel.timeline.takeHeal(now);
                if (amount > 0) UtilPlayer.health(player, amount);
                if (channels.get(player) != channel) break; // healing listeners may invalidate the skill
                particles(channel.anchor, channel.timeline.started, true, danger);
                playHeal(player);
            }
            if (channels.get(player) != channel) continue;
            if (channel.timeline.isComplete(now)) {
                end(player, true);
            } else if (now >= channel.nextParticles) {
                particles(channel.anchor, channel.timeline.started, false, danger);
                if (danger > 0) {
                    // Pulse climbs in pitch as the burst nears its cap - the tell for anyone still hitting.
                    channel.anchor.getWorld().playSound(channel.anchor, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                            0.5f, 0.6f + (float) danger);
                }
                channel.nextParticles = now + (long) (200 - danger * 110); // pulses quicken as it fills
            }
        }
        pendingBursts.entrySet().removeIf(entry -> {
            PendingBurst burst = entry.getValue();
            long left = burst.fireAt - now;
            if (left > 0) {
                telegraphParticles(burst.center, 1 - (double) left / Math.max(1, burstTelegraph));
                return false;
            }
            detonate(entry.getKey(), burst.center, burst.charge);
            return true;
        });
        protection.entrySet().removeIf(entry -> now >= entry.getValue().expires || !isEnabled()
                || !entry.getKey().isOnline() || entry.getKey().isDead() || getLevel(entry.getKey()) <= 0);
    }

    private void end(Player player, boolean successful) {
        Channel channel = channels.remove(player);
        if (channel == null) return;
        channel.timeline.end();
        channel.bar.remove(channel.display);
        if (!player.isOnline()) return;
        player.setVelocity(new Vector());
        if (!player.isDead() && !championsManager.getCooldowns().hasCooldown(player, getName())) {
            championsManager.getCooldowns().use(player, getName(), channel.cooldown, showCooldownFinished(),
                    true, isCancellable(), this::shouldDisplayActionBar, getPriority());
        }
        if (successful && !player.isDead()) {
            if (channel.exitDuration > 0 && channel.exitReduction > 0) {
                protection.put(player, new Protection(now() + channel.exitDuration, channel.exitReduction));
            }
            if (burstDamage(channel.charge) > 0) {
                // Fuse it rather than firing instantly - a short wind-up so anyone in range can sprint clear.
                pendingBursts.put(player, new PendingBurst(player.getLocation().clone(), channel.charge,
                        now() + burstTelegraph));
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.6f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.5f);
            }
            playEnd(player);
        }
    }

    /** Imploding black ring during the fuse, then a black shockwave: capped damage + a shove to everyone near. */
    private void detonate(Player source, Location center, double charge) {
        double damage = burstDamage(charge);
        if (damage <= 0) return;
        for (LivingEntity target : UtilEntity.getNearbyEnemies(source, center, burstRadius)) {
            if (target.equals(source)) continue;
            Vector trajectory = UtilVelocity.getTrajectory2d(center.toVector(), target.getLocation().toVector());
            UtilVelocity.velocity(target, source, new VelocityData(trajectory, burstKnockback, 0.25, 0.6, true));
            UtilDamage.doDamage(new DamageEvent(target, source, null, new SkillDamageCause(this), damage, getName()));
        }
        Location core = center.clone().add(0, 1, 0);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.5f);
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.45f, 1.3f);
        Particle.SQUID_INK.builder().location(core).count(45).offset(0.35, 0.4, 0.35).extra(0.18).receivers(45).spawn();
        Particle.SMOKE.builder().location(core).count(40).offset(0.2, 0.2, 0.2).extra(0.08).receivers(45).spawn();
        for (double radius = 0.6; radius <= burstRadius; radius += 0.8) {
            for (int i = 0; i < 22; i++) {
                double a = Math.PI * 2 * i / 22;
                Particle.DUST.builder()
                        .location(center.clone().add(Math.cos(a) * radius, 0.25, Math.sin(a) * radius))
                        .data(new Particle.DustOptions(Color.fromRGB(14, 14, 16), 1.5f))
                        .count(1).extra(0).receivers(45).spawn();
            }
        }
    }

    private void telegraphParticles(Location center, double progress) {
        double radius = burstRadius * (1 - Math.max(0, Math.min(1, progress))); // ring collapses inward as the fuse burns
        for (int i = 0; i < 20; i++) {
            double a = Math.PI * 2 * i / 20;
            Particle.DUST.builder()
                    .location(center.clone().add(Math.cos(a) * radius, 0.25, Math.sin(a) * radius))
                    .data(new Particle.DustOptions(Color.fromRGB(20, 20, 24), 1.2f))
                    .count(1).extra(0).receivers(40).spawn();
        }
    }

    private double burstDamage(double charge) {
        if (charge <= 0 || burstReflectRatio <= 0 || burstRadius <= 0) return 0;
        return Math.min(burstMaxDamage, charge * burstReflectRatio);
    }

    private double dangerFraction(Channel channel) {
        if (burstMaxDamage <= 0) return channel.charge > 0 ? 1 : 0;
        return Math.max(0, Math.min(1, burstDamage(channel.charge) / burstMaxDamage));
    }

    private void clear(Player player) {
        end(player, false);
        protection.remove(player);
        pendingBursts.remove(player); // a forced exit (death, teleport, unequip) drops the fuse
    }

    private boolean locked(Entity entity) { return entity instanceof Player player && channels.containsKey(player); }

    private boolean lockedSource(Entity entity) {
        return locked(entity) || entity instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player && locked(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageEarly(DamageEvent event) {
        if (locked(event.getDamagee()) || lockedSource(event.getDamager()) || lockedSource(event.getDirectEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * Every blow blocked by a meditating player feeds the exit burst. Runs once at MONITOR (not re-invoked like
     * onDamageEarly) and reads the intended damage even though the event is already cancelled.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMeditationCharge(DamageEvent event) {
        if (!(event.getDamagee() instanceof Player player) || !playerSourced(event)) return;
        Channel channel = channels.get(player);
        if (channel == null) return;
        channel.charge += Math.max(0, event.getDamage());
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.6f, 1.5f);
        Particle.DUST.builder().location(player.getLocation().add(0, 1, 0))
                .data(new Particle.DustOptions(Color.fromRGB(255, 120, 60), 1.1f))
                .count(10).offset(0.35, 0.45, 0.35).extra(0).receivers(30).spawn();
    }

    private boolean playerSourced(DamageEvent event) {
        if (event.getDamager() instanceof Player) return true;
        return event.getDirectEntity() instanceof Projectile projectile && projectile.getShooter() instanceof Player;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageLate(DamageEvent event) {
        onDamageEarly(event); // catch damage whose owner was resolved by another listener
        if (event.isCancelled() || !(event.getDamagee() instanceof Player player)) return;
        Protection exit = protection.get(player);
        if (exit != null && now() < exit.expires) {
            event.addModifier(new SkillDamageModifier.Multiplier(this, 1 - exit.reduction));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVanillaDamage(EntityDamageEvent event) {
        // Entity hits are left alone here so they reach BetterPvP's DamageEvent pipeline - onDamageEarly
        // cancels them there (no damage) and the burst meter reads them. This only stops environmental
        // damage, which never becomes a DamageEvent.
        if (event instanceof EntityDamageByEntityEvent) return;
        if (locked(event.getEntity()) || lockedSource(event.getDamageSource().getCausingEntity())
                || lockedSource(event.getDamageSource().getDirectEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUseSkill(PlayerUseSkillEvent event) {
        if (locked(event.getPlayer()) && event.getSkill() != this) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCanUseSkill(PlayerCanUseSkillEvent event) {
        if (locked(event.getPlayer()) && event.getSkill() != this) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent) return;
        Channel channel = channels.get(event.getPlayer());
        if (channel == null || event.getTo() == null || !event.hasChangedPosition()) return;
        Location destination = channel.anchor.clone();
        destination.setYaw(event.getTo().getYaw());
        destination.setPitch(event.getTo().getPitch());
        // Cancelling uses the movement rollback path. setTo would cause a plugin teleport and end the channel.
        event.setFrom(destination);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVelocity(PlayerVelocityEvent event) {
        if (locked(event.getPlayer())) {
            event.setVelocity(new Vector());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCustomVelocity(CustomEntityVelocityEvent event) {
        if (locked(event.getEntity())) {
            event.setVector(new Vector());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttack(PrePlayerAttackEntityEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!locked(event.getPlayer())) return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityInteractAt(PlayerInteractAtEntityEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (locked(event.getWhoClicked())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (locked(event.getWhoClicked())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (lockedSource(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        // SkillListener handles the Sword / Axe reactivation first; suppress any other item drops.
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) { clear(event.getPlayer()); }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) { clear(event.getPlayer()); }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) { clear(event.getEntity()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { clear(event.getPlayer()); }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) { clear(player); }

    @Override
    public void reload() {
        List.copyOf(channels.keySet()).forEach(this::clear);
        protection.clear();
        pendingBursts.clear();
        super.reload();
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() != champions) return;
        List.copyOf(channels.keySet()).forEach(this::clear);
        protection.clear();
        pendingBursts.clear();
    }

    protected long now() { return System.currentTimeMillis(); }

    protected void playActivation(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
    }

    protected void playHeal(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.25f, 1.5f);
    }

    protected void playEnd(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.4f, 1.6f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.35f, 1.4f);
    }

    protected void particles(Location anchor, long started, boolean heal, double danger) {
        double phase = (now() - started) / 1000.0;
        danger = Math.max(0, Math.min(1, danger));
        // Calm blue orbit, tinting toward an ominous dark red as the burst charges.
        Color color = Color.fromRGB(
                (int) Math.round((heal ? 160 : 100) + (150 - (heal ? 160 : 100)) * danger),
                (int) Math.round(210 + (10 - 210) * danger),
                (int) Math.round(230 + (10 - 230) * danger));
        for (int i = 0; i < 16; i++) {
            double angle = phase + i * Math.PI / 8;
            Particle.DUST.builder().location(anchor.clone().add(Math.cos(angle) * 0.8, 0.1, Math.sin(angle) * 0.8))
                    .data(new Particle.DustOptions(color, 0.9f))
                    .count(1).extra(0).receivers(30).spawn();
        }
        Particle.END_ROD.builder().location(anchor.clone().add(0, 0.8, 0))
                .count(heal ? 5 : 2).offset(0.35, 0.6, 0.35).extra(0.015).receivers(30).spawn();
    }

    @Override
    public void loadSkillConfig() {
        duration = milliseconds("duration", 4, 0.05);
        interval = milliseconds("healingInterval", 0.5, 0.05);
        finalPhase = Math.min(duration, milliseconds("finalPhaseDuration", 1, 0));
        cancelDelay = Math.min(duration, milliseconds("minimumCancelTime", 1, 0));
        exitDuration = milliseconds("exitResistanceDuration", 0.5, 0);
        healing = number("healingPerTick", 1, 0);
        finalHealing = number("finalHealingPerTick", 2, 0);
        exitReduction = Math.min(1, number("exitDamageReduction", 0.2, 0));
        burstReflectRatio = number("burstReflectRatio", 0.6, 0);
        burstMaxDamage = number("burstMaxDamage", 12, 0);
        burstRadius = number("burstRadius", 4, 0);
        burstKnockback = number("burstKnockback", 1.2, 0);
        burstTelegraph = milliseconds("burstTelegraph", 0.5, 0);
        cooldown = Double.isFinite(cooldown) ? Math.max(0, cooldown) : 24;
    }

    private double number(String key, double fallback, double minimum) {
        double value = getConfig(key, fallback, Number.class).doubleValue();
        return Double.isFinite(value) ? Math.max(minimum, value) : fallback;
    }

    @Override
    protected <T> T getConfig(String name, T defaultValue, Class<T> type) {
        // Skill's generic defaults are five levels and a one-second cooldown. Seed this new skill's
        // actual defaults when an existing server has no meditation section yet.
        Object fallback = switch (name) {
            case "maxlevel" -> 1;
            case "cooldown" -> 24.0;
            case "cooldownDecreasePerLevel" -> 0.0;
            default -> defaultValue;
        };
        return super.getConfig(name, type.cast(fallback), type);
    }

    private long milliseconds(String key, double fallback, double minimum) {
        return Math.round(number(key, fallback, minimum) * 1000);
    }

    private static final class Channel {
        private final Location anchor;
        private final MeditationTimeline timeline;
        private final ActionBar bar;
        private final double cooldown;
        private final long exitDuration;
        private final double exitReduction;
        private TimedComponent display;
        private long nextParticles;
        private double charge; // total damage blocked, released as the exit burst

        private Channel(Location anchor, MeditationTimeline timeline, ActionBar bar, double cooldown,
                        long exitDuration, double exitReduction) {
            this.anchor = anchor;
            this.timeline = timeline;
            this.bar = bar;
            this.cooldown = cooldown;
            this.exitDuration = exitDuration;
            this.exitReduction = exitReduction;
        }
    }

    /** Per-cast healing schedule. Uses scheduled tick times so lag does not change the healing total. */
    static final class MeditationTimeline {
        final long started;
        final long duration;
        private final long interval;
        private final long finalPhase;
        private final long cancelDelay;
        private final double healing;
        private final double finalHealing;
        private long nextTick = 1;
        private boolean ended;

        MeditationTimeline(long started, long duration, long interval, long finalPhase, long cancelDelay,
                           double healing, double finalHealing) {
            this.started = started;
            this.duration = duration;
            this.interval = interval;
            this.finalPhase = finalPhase;
            this.cancelDelay = cancelDelay;
            this.healing = healing;
            this.finalHealing = finalHealing;
        }

        boolean canCancel(long now) {
            return !ended && now - started >= cancelDelay;
        }

        boolean isComplete(long now) {
            return now - started >= duration;
        }

        boolean hasDueHeal(long now) {
            return !ended && nextTick * interval <= Math.min(duration, now - started);
        }

        double takeHeal(long now) {
            if (!hasDueHeal(now)) throw new IllegalStateException("No Meditation healing tick is due");
            return nextTick++ * interval > duration - finalPhase ? finalHealing : healing;
        }

        double totalHealing() {
            long ticks = duration / interval;
            long normalTicks = (duration - finalPhase) / interval;
            return normalTicks * healing + (ticks - normalTicks) * finalHealing;
        }

        void end() {
            ended = true;
        }
    }

    private record Protection(long expires, double reduction) { }

    private record PendingBurst(Location center, double charge, long fireAt) { }
}
