package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scythe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("ALL")
@BPvPListener
@Singleton
@CustomLog
public class ScytheListener implements Listener {

    private final Scythe scythe;
    private final EffectManager effectManager;

    @Inject
    public ScytheListener(Scythe scythe, EffectManager effectManager) {
        this.scythe = scythe;
        this.effectManager = effectManager;
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(PreDamageEvent event) {
        if(event.isCancelled()) return;
        if(!scythe.isEnabled()) {
            return;
        }

        DamageEvent cde = event.getDamageEvent();
        if (cde.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(cde.getDamager() instanceof Player damager)) return;
        if (scythe.isHoldingWeapon(damager) && scythe.tracked.containsKey(damager)) {
            final double soulCount = scythe.tracked.get(damager).getSoulCount();
            cde.setDamage(scythe.getBaseDamage() + scythe.maxSoulsDamage * soulCount / scythe.maxSouls);
        }
    }

    @EventHandler
    public void onHeal(CustomDamageEvent event) {
        if(event.isCancelled()) return;
        if(!scythe.isEnabled()) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (scythe.isHoldingWeapon(damager) && scythe.tracked.containsKey(damager)) {
            final double soulCount = scythe.tracked.get(damager).getSoulCount();
            UtilPlayer.health(damager, scythe.baseHeal + scythe.healPerSoul * soulCount);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if(!scythe.isEnabled()) {
            return;
        }

        final PlayerInventory inventory = event.getPlayer().getInventory();
        if (scythe.matches(inventory.getItemInMainHand()) || scythe.matches(inventory.getItemInOffHand())) {
            scythe.active(event.getPlayer());
        }
    }

    @EventHandler
    public void onSwapWeapon(PlayerItemHeldEvent event) {
        if(!scythe.isEnabled()) {
            return;
        }

        final Player player = event.getPlayer();
        if (scythe.matches(player.getInventory().getItem(event.getNewSlot()))) {
            scythe.active(player);
        } else if (scythe.tracked.containsKey(player)) {
            scythe.pause(player, scythe.tracked.get(player));
        }
    }

    @EventHandler
    public void onSlotChange(PlayerInventorySlotChangeEvent event) {
        if(!scythe.isEnabled()) {
            return;
        }

        if (scythe.matches(event.getNewItemStack())) {
            scythe.active(event.getPlayer());
        } else if (scythe.matches(event.getOldItemStack()) && Arrays.stream(event.getPlayer().getInventory().getContents()).noneMatch(scythe::matches)) {
            scythe.deactivate(event.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(!scythe.isEnabled()) {
            return;
        }

        // Remove all souls from the player who died
        final Player damagee = event.getEntity();
        if  (scythe.tracked.containsKey(damagee)) {
            scythe.tracked.get(damagee).setSoulCount(0);
            scythe.tracked.get(damagee).setHarvesting(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(CustomDamageEvent event) {
        if(!scythe.isEnabled()) {
            return;
        }

        if (event.getDamagee() instanceof ArmorStand) {
            return;
        }

        if(event.getDamagee().hasMetadata("PlayerSpawned")) {
            return;
        }

        if (!event.getDamagee().isDead()) {
            return; // Entity didn't die
        }

        // Remove all in-world souls from the entity who died
        scythe.souls.values().stream()
                .filter(soul -> soul.getOwner().equals(event.getDamagee().getUniqueId()))
                .forEach(soul -> soul.setMarkForRemoval(true));

        // If the entity was killed by a scythe, give the killer a soul
        final double soulCount = getSoulCount(event.getDamagee());
        if (event.getDamager() instanceof Player attacker && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && scythe.isHoldingWeapon(attacker)
                && event.getDamagee() instanceof Player) {

            final ScytheData data = scythe.tracked.get(attacker);
            final boolean success = data.gainSoul(soulCount);
            if (success) {
                data.playHarvest(null);
            }
            return;
        }

        if (!(event.getDamager() instanceof Player) && !(event.getDamagee() instanceof Player)) {
            // Return if an entity didn't die to a player
            // Players attempt spawning souls 100% of the time
            // Mobs attempt spawning souls only when players kill them
            return;
        }

        // Otherwise, spawn a soul
        trySummonSoul(event.getDamagee(), soulCount);
    }

    private double getSoulCount(LivingEntity damagee) {
        return damagee instanceof Player ? this.scythe.soulsPerPlayer : this.scythe.soulsPerMob;
    }

    @UpdateEvent
    public void doScythe() {
        if(!scythe.isEnabled()) {
            return;
        }

        final Iterator<Map.Entry<Player, ScytheData>> iterator = scythe.tracked.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, ScytheData> cur = iterator.next();
            final Player player = cur.getKey();
            final ScytheData data = cur.getValue();
            if (player == null) {
                iterator.remove();
                continue;
            }

            // Expire souls for everyone constantly
            double soulCount = data.getSoulCount();
            if (data.tryExpireSoul(scythe.soulExpiryPerSecond / 20d)) {
                double newSoulCount = data.getSoulCount();
                if ((int) soulCount != (int) newSoulCount) {
                    data.playLoseSoul();
                }
            }

            // Remove them from cache if they have no souls and are scheduled for removal
            if (data.isMarkForRemoval() && data.getSoulCount() == 0) {
                iterator.remove();
                continue;
            }

            // Effects
            data.playPassive();
            final int speedLevel = (int) (scythe.speedAmplifierPerSoul * (data.getSoulCount()));
            if (speedLevel > 0) {
                effectManager.addEffect(player, EffectTypes.SPEED, "Scythe", speedLevel, 150, true);
            }

            // Stop harvesting if they're not holding the weapon
            final Gamer gamer = data.getGamer();
            if (!scythe.isHoldingWeapon(player)) {
                data.stopHarvesting();
                continue;
            }

            // Deselect previous target
            Soul target = scythe.getTargetSoul(player).orElse(null);
            Soul oldTarget = data.getTargetSoul();
            if (target != oldTarget) {
                if (oldTarget != null) {
                    oldTarget.show(player, false, scythe);
                }

                if (target != null) {
                    target.show(player, true, scythe);
                }
            }

            // Select new target
            data.setTargetSoul(target);

            // Stop harvesting if they're not holding right click
            if (!gamer.isHoldingRightClick()) {
                data.stopHarvesting();
                continue;
            }

            // Call usage event
            var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(player, scythe, true));
            if (checkUsageEvent.isCancelled()) {
                UtilMessage.simpleMessage(player, "Restriction", "You cannot use this weapon here.");
                iterator.remove();
                scythe.pause(player, data);
                continue;
            }

            // Play particles if they're harvesting or just finished harvesting
            if (data.isHarvesting()) {
                final Soul soul = data.getTargetSoul();
                boolean finished = data.attemptHarvest();
                if (!finished) {
                    data.playHarvestProgress();
                } else {
                    data.playHarvest(soul);
                    scythe.souls.remove(soul.getUniqueId());
                    Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(Champions.class), soul.getDisplay()::remove, 2);
                }
            } else if (target != null && !target.isHarvesting()) {

                // If they're not harvesting, but they are clicking in the direction of a soul, start harvesting
                data.startHarvesting();
                data.playHarvestStart();
            }

        }

        // Play particles on all souls
        final Iterator<Soul> soulsIterator = scythe.souls.values().iterator();
        while (soulsIterator.hasNext()) {
            final Soul soul = soulsIterator.next();
            // Remove souls that have expired and aren't being harvested
            if (!soul.isHarvesting() && System.currentTimeMillis() - soul.getSpawnTime() >= scythe.soulDespawnSeconds * 1000) {
                soul.setMarkForRemoval(true);
            }

            if (soul.isMarkForRemoval()) {
                soul.getDisplay().remove();
                soulsIterator.remove();
            }

            // otherwise play particles
            soul.play(scythe);
        }
    }

    protected void trySummonSoul(LivingEntity entity, double soulCount) {
        if (entity instanceof ArmorStand) {
            return;
        }

        // Check if we should summon a soul
        final double chance = Math.random();
        if (entity instanceof Player) {
            if (chance > scythe.summonPlayerSoulChance) {
                return;
            }
        } else if (chance > scythe.summonMobSoulChance) {
            return;
        }

        final Location location = entity.getLocation().add(0, entity.getHeight(), 0);
        final long time = System.currentTimeMillis();
        final ItemDisplay display = (ItemDisplay) entity.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY, CreatureSpawnEvent.SpawnReason.CUSTOM, ent -> {
            final ItemDisplay spawned = (ItemDisplay) ent;
            spawned.setVisibleByDefault(false);
            spawned.setViewRange(scythe.soulViewDistanceBlocks);
            spawned.setInterpolationDelay(8);
            spawned.setInterpolationDelay(0);
            spawned.setBillboard(Display.Billboard.CENTER);
            spawned.setGlowing(true);
            spawned.setPersistent(false);
            UtilEntity.setViewRangeBlocks(spawned, scythe.soulViewDistanceBlocks);

            final Transformation transformation = new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(1, 1, 1),
                    new Quaternionf(0, -1, 0, 0)
            );
            spawned.setTransformation(transformation);

            if (entity instanceof Player) {
//                try {
//                    final ItemStack item = new SkullBuilder(entity.getUniqueId()).get();
//                    spawned.setItemStack(new ItemStack(Material.SKELETON_SKULL));
//                } catch (MojangApiUtils.MojangApiException | IOException e) {
//                    spawned.setItemStack(new ItemStack(Material.SKELETON_SKULL));
//                    log.error("Failed to bind head texture for soul (" + ent.getName() + ")", e).submit();
//                }
                spawned.setItemStack(new ItemStack(Material.SKELETON_SKULL));
            } else {
                spawned.setItemStack(new ItemStack(Material.SKELETON_SKULL));
            }
        });
        final Soul soul = new Soul(display, entity.getUniqueId(), location, time, soulCount);
        scythe.souls.put(soul.getUniqueId(), soul);

        // Show to everyone who can see the soul
        for (Player toShow : scythe.tracked.keySet()) {
            if (!scythe.isHoldingWeapon(toShow)) {
                continue;
            }
            soul.show(toShow, false, scythe);
        }
    }

}
