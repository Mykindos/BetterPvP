package me.mykindos.betterpvp.champions.item.scythe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbilityDamageModifier;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Singleton
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@BPvPListener
public class SoulHarvestAbility extends ItemAbility implements Listener {

    private int maxSouls;
    private double soulHarvestSeconds;
    private double soulExpirySeconds;
    private double soulExpiryPerSecond;
    private double soulDespawnSeconds;
    private int soulViewDistanceBlocks;
    private double maxSoulsDamage;
    private double soulsPerPlayer;
    private double soulsPerMob;
    private double summonPlayerSoulChance;
    private double summonMobSoulChance;
    private double speedAmplifierPerSoul;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    @EqualsAndHashCode.Exclude
    private final DamageLogManager damageLogManager;
    @EqualsAndHashCode.Exclude
    private final EffectManager effectManager;
    @EqualsAndHashCode.Exclude
    private final ClientManager clientManager;
    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;
    @EqualsAndHashCode.Exclude
    private final Map<UUID, Soul> souls = new HashMap<>();
    @EqualsAndHashCode.Exclude
    private final Map<UUID, ScytheData> playerData = new HashMap<>();
    @EqualsAndHashCode.Exclude
    private ScytheOfTheFallenLord scythe;

    @EqualsAndHashCode.Exclude
    private final DisplayObject<Component> actionBar = ChargeData.getActionBar(
            gmr -> gmr.isOnline() && playerData.containsKey(gmr.getPlayer().getUniqueId()) && scythe.isHoldingWeapon(gmr.getPlayer()),
            gmr -> playerData.get(gmr.getPlayer().getUniqueId()).getChargeData()
    );

    @Inject
    private SoulHarvestAbility(Champions champions, DamageLogManager damageLogManager, EffectManager effectManager, ClientManager clientManager, ItemFactory itemFactory) {
        super(new NamespacedKey(champions, "soul_harvest"),
                "Soul Harvest",
                "Collect souls of fallen players and mobs to harvest their souls, gaining damage and speed.",
                TriggerTypes.HOLD_BLOCK);
        this.champions = champions;
        this.damageLogManager = damageLogManager;
        this.effectManager = effectManager;
        this.clientManager = clientManager;
        this.itemFactory = itemFactory;
        
        // Default values, will be overridden by config
        this.maxSouls = 3;
        this.soulHarvestSeconds = 1.5;
        this.soulExpirySeconds = 10.0;
        this.soulExpiryPerSecond = 0.3;
        this.soulDespawnSeconds = 7.5;
        this.soulViewDistanceBlocks = 60;
        this.maxSoulsDamage = 4.0;
        this.soulsPerPlayer = 1.0;
        this.soulsPerMob = 1.0;
        this.summonPlayerSoulChance = 1.0;
        this.summonMobSoulChance = 0.4;
        this.speedAmplifierPerSoul = 1.0;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        final ScytheData data = playerData.get(player.getUniqueId());
        if (data == null) {
            return false;
        }

        final Soul target = getTargetSoul(player).orElse(null);

        // Call usage event
        var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(player, itemInstance, true));
        if (checkUsageEvent.isCancelled()) {
            UtilMessage.simpleMessage(player, "Restriction", "You cannot use this weapon here.");
            playerData.remove(player.getUniqueId());
            pause(player, data);
            return false;
        }

        // Play particles if they're harvesting or just finished harvesting
        if (data.isHarvesting()) {
            final Soul soul = data.getTargetSoul();
            boolean finished = data.attemptHarvest();
            if (!finished) {
                data.playHarvestProgress();
            } else {
                data.playHarvest(soul);
                souls.remove(soul.getUniqueId());
                Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(Champions.class), soul.getDisplay()::remove, 2);
            }
            return true;
        } else if (target != null && !target.isHarvesting()) {
            // If they're not harvesting, but they are clicking in the direction of a soul, start harvesting
            data.startHarvesting();
            data.playHarvestStart();
            return true;
        }
        return false;
    }
    
    /**
     * Find a soul that the player is looking at
     */
    public Optional<Soul> getTargetSoul(Player player) {
        for (Soul soul : souls.values()) {
            final Location loc = soul.getLocation();
            final BoundingBox box = new BoundingBox(loc.getX() - 1,
                    loc.getY() - 1,
                    loc.getZ() - 1,
                    loc.getX() + 1,
                    loc.getY() + 1,
                    loc.getZ() + 1);

            final RayTraceResult trace = box.rayTrace(player.getEyeLocation().toVector(),
                    player.getLocation().getDirection(),
                    soulViewDistanceBlocks);

            if (trace != null) {
                return Optional.of(soul);
            }
        }

        return Optional.empty();
    }

    private void pause(Player player, ScytheData data) {
        data.stopHarvesting();
        data.getGamer().getActionBar().remove(actionBar);

        for (Soul soul : souls.values()) {
            soul.hide(player);
        }
    }

    private void deactivate(Player player) {
        final ScytheData data = playerData.get(player.getUniqueId());
        if (data != null) {
            data.setMarkForRemoval(true);
            pause(player, data);
        }
    }
    
    /**
     * Track player for soul collection and management
     */
    private void active(Player player) {
        playerData.compute(player.getUniqueId(),
                (playerId, prev) -> {
                    if (prev != null) {
                        prev.setMarkForRemoval(false);
                        prev.getGamer().getActionBar().add(350, actionBar);
                        return prev;
                    }
                    final ScytheData data = new ScytheData(scythe, clientManager.search().online(playerId).orElseThrow().getGamer());
                    data.getGamer().getActionBar().add(350, actionBar);
                    return data;
                });

        for (Soul soul : souls.values()) {
            soul.show(player, false, scythe);
        }
    }

    /**
     * Clear soul data for a player
     */
    public void clearSoulData(Player player) {
        ScytheData data = playerData.get(player.getUniqueId());
        if (data != null) {
            data.setSoulCount(0);
            data.setHarvesting(false);
        }
    }
    
    /**
     * Mark all souls from a specific entity for removal
     */
    public void markSoulsForRemoval(UUID entityId) {
        souls.values().stream()
                .filter(soul -> soul.getOwner().equals(entityId))
                .forEach(soul -> soul.setMarkForRemoval(true));
    }
    
    /**
     * Give souls directly to a player without spawning a physical soul
     */
    public void grantSoulsToPlayer(Player player, double count) {
        ScytheData data = playerData.get(player.getUniqueId());
        if (data != null && data.gainSoul(count)) {
            data.playHarvest(null);
        }
    }


    /**
     * Get the soul count for a player
     */
    private double getSoulCount(LivingEntity damagee) {
        return damagee instanceof Player ? this.soulsPerPlayer : this.soulsPerMob;
    }

    /**
     * Update soul data for all players
     */
    @UpdateEvent(priority = 999)
    public void doScythe() {
        final Iterator<Map.Entry<UUID, ScytheData>> iterator = playerData.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<UUID, ScytheData> cur = iterator.next();
            final UUID playerId = cur.getKey();
            final ScytheData data = cur.getValue();
            final Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            // Expire souls for everyone constantly
            double soulCount = data.getSoulCount();
            if (data.tryExpireSoul(soulExpiryPerSecond / 20d)) {
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
            final int speedLevel = (int) (speedAmplifierPerSoul * (data.getSoulCount()));
            if (speedLevel > 0) {
                effectManager.addEffect(player, EffectTypes.SPEED, "Scythe", speedLevel, 150, true);
            }

            // Stop harvesting if they're not holding the weapon
            final Gamer gamer = data.getGamer();
            ItemInstance scytheInstance = scythe.getScytheInstance(player);
            if (scytheInstance == null) {
                data.stopHarvesting();
                continue;
            }

            // Deselect previous target
            Soul target = getTargetSoul(player).orElse(null);
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
            }
        }

        // Play particles on all souls
        final Iterator<Soul> soulsIterator = souls.values().iterator();
        while (soulsIterator.hasNext()) {
            final Soul soul = soulsIterator.next();
            // Remove souls that have expired and aren't being harvested
            if (!soul.isHarvesting() && System.currentTimeMillis() - soul.getSpawnTime() >= soulDespawnSeconds * 1000) {
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

    /**
     * Clean up data when player quits
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerData.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Track player if they're holding the Scythe when joining
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (scythe == null) return;

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        // Check if player is holding the Scythe
        itemFactory.fromItemStack(mainHand).ifPresent(item -> {
            if (item.getBaseItem() == scythe) {
                active(player);
            }
        });
    }

    @EventHandler
    public void onSlotChange(PlayerInventorySlotChangeEvent event) {
        if (scythe.isScythe(event.getNewItemStack())) {
            active(event.getPlayer());
        } else if (scythe.isScythe(event.getOldItemStack()) && Arrays.stream(event.getPlayer().getInventory().getContents()).noneMatch(scythe::isScythe)) {
            deactivate(event.getPlayer());
        }
    }


    @EventHandler
    public void onSwapWeapon(PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        if (scythe.isScythe(player.getInventory().getItem(event.getNewSlot()))) {
            active(event.getPlayer());
        } else if (playerData.containsKey(player.getUniqueId())) {
            pause(event.getPlayer(), playerData.get(player.getUniqueId()));
        }
    }

    /**
     * Clear soul data when a player dies
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        clearSoulData(event.getEntity());
    }

    /**
     * Create souls when entities die
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof ArmorStand) {
            return;
        }

        if(event.getEntity().hasMetadata("PlayerSpawned")) {
            return;
        }

        // Remove all in-world souls from the entity who died
        souls.values().stream()
                .filter(soul -> soul.getOwner().equals(event.getEntity().getUniqueId()))
                .forEach(soul -> soul.setMarkForRemoval(true));

        DamageLog lastDamager = damageLogManager.getLastDamager(event.getEntity());
        if (lastDamager == null) {
            return;
        }

        // If the entity was killed by a scythe, give the killer a soul
        final double soulCount = getSoulCount(event.getEntity());
        final DamageCause cause = lastDamager.getDamageCause();
        if (lastDamager.getDamager() instanceof Player attacker && cause.getCategories().contains(DamageCauseCategory.MELEE) && scythe.isHoldingWeapon(attacker)
                && event.getEntity() instanceof Player) {
            final ScytheData data = playerData.get(attacker.getUniqueId());
            final boolean success = data.gainSoul(soulCount);
            if (success) {
                data.playHarvest(null);
            }
            return;
        }

        if (!(lastDamager.getDamager() instanceof Player) && !(event.getEntity() instanceof Player)) {
            // Return if an entity didn't die to a player
            // Players attempt spawning souls 100% of the time
            // Mobs attempt spawning souls only when players kill them
            return;
        }

        // Otherwise, spawn a soul
        trySummonSoul(event.getEntity(), soulCount);
    }

    /**
     * Apply bonus damage based on soul count
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(DamageEvent event) {
        if (event.isCancelled()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        // Check if player is holding this weapon
        itemFactory.fromItemStack(damager.getInventory().getItemInMainHand()).ifPresent(item -> {
            if (item.getBaseItem() != scythe) return;

            // Apply bonus damage based on soul count
            double soulCount =  getPlayerData().get(damager.getUniqueId()).getSoulCount();
            double maxSouls = getMaxSouls();

            // Calculate and apply bonus damage
            double bonusDamage = getMaxSoulsDamage() * soulCount / maxSouls;
            if (bonusDamage > 0) {
                event.addModifier(new ItemAbilityDamageModifier.Flat(this, bonusDamage));
            }
        });
    }

    /**
     * Summon a soul at an entity's location
     */
    protected void trySummonSoul(LivingEntity entity, double soulCount) {
        if (entity instanceof ArmorStand) {
            return;
        }

        // Check if we should summon a soul
        final double chance = Math.random();
        if (entity instanceof Player) {
            if (chance > summonPlayerSoulChance) {
                return;
            }
        } else if (chance > summonMobSoulChance) {
            return;
        }

        final Location location = entity.getLocation().add(0, entity.getHeight(), 0);
        final long time = System.currentTimeMillis();
        final ItemDisplay display = (ItemDisplay) entity.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY, CreatureSpawnEvent.SpawnReason.CUSTOM, ent -> {
            final ItemDisplay spawned = (ItemDisplay) ent;
            spawned.setVisibleByDefault(false);
            spawned.setViewRange(soulViewDistanceBlocks);
            spawned.setInterpolationDelay(8);
            spawned.setInterpolationDelay(0);
            spawned.setBillboard(Display.Billboard.CENTER);
            spawned.setGlowing(true);
            spawned.setPersistent(false);
            UtilEntity.setViewRangeBlocks(spawned, soulViewDistanceBlocks);

            final Transformation transformation = new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(1, 1, 1),
                    new Quaternionf(0, -1, 0, 0)
            );
            spawned.setTransformation(transformation);
            spawned.setItemStack(new ItemStack(Material.SKELETON_SKULL));
        });
        final Soul soul = new Soul(display, entity.getUniqueId(), location, time, soulCount);
        souls.put(soul.getUniqueId(), soul);

        // Show to everyone who can see the soul
        for (UUID playerId : playerData.keySet()) {
            final Player player = Bukkit.getPlayer(playerId);
            if (!scythe.isHoldingWeapon(player)) {
                continue;
            }
            soul.show(player, false, scythe);
        }
    }
}