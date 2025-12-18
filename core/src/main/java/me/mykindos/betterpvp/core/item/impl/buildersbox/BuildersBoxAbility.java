package me.mykindos.betterpvp.core.item.impl.buildersbox;

import com.destroystokyo.paper.ParticleBuilder;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.entity.Dummy;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.item.DroppedItemLoot;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Creates a delayed item explosion.
 */
public class BuildersBoxAbility extends ItemAbility {

    private final Supplier<LootTable> lootTableSupplier;
    private final String source;

    public BuildersBoxAbility(Supplier<LootTable> lootTableSupplier, String source) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Core.class), "builders_box_explosion"),
              "Open",
              "Unseal the box to release a trove of building items.",
              TriggerTypes.RIGHT_CLICK);
        this.lootTableSupplier = lootTableSupplier;
        this.source = source;
        this.setConsumesItem(true);
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        final Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        final double reach = Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)).getValue();

        // Get where to spawn it
        final RayTraceResult rayTrace = player.rayTraceBlocks(reach);
        final Location location = rayTrace != null
                ? rayTrace.getHitPosition().toLocation(player.getWorld())
                : player.getEyeLocation().add(player.getLocation().getDirection().multiply(reach));
        location.setDirection(player.getEyeLocation().getDirection().multiply(-1).setY(0)); // Looking toward the player

        // Generate Loot
        final LootTable lootTable = lootTableSupplier.get();
        final LootSession lootSession = LootSession.newSession(lootTable, player);
        final LootContext context = new LootContext(lootSession, location.clone().add(0, 0.9, 0), source);
        final LootBundle bundle = lootTable.generateLoot(context);
        final Iterator<Loot<?, ?>> iterator = bundle.iterator();

        // Spawn the model using ModelEngine
        final Dummy<?> dummy = new Dummy<>();
        dummy.setVisible(true);
        dummy.getBodyRotationController().setRotationDuration(0);
        dummy.getBodyRotationController().setRotationDelay(0);
        dummy.syncLocation(location);
        dummy.setRenderRadius(512);
        final ActiveModel activeModel = ModelEngineAPI.createActiveModel("builders_box");
        ModelEngineAPI.createModeledEntity(dummy, created -> {
            created.addModel(activeModel, true);
            created.setModelRotationLocked(false);
        });

        // Play the fall and open animation
        // Make sure it holds the last frame so it stays opened
        final long fallDuration = (long) (0.88 * 20L);
        final long openDuration = (long) (4.5 * 20L);

        final AnimationHandler animationHandler = activeModel.getAnimationHandler();
        animationHandler.playAnimation("fall", 0, 0, 1, true);
        Objects.requireNonNull(animationHandler.getAnimation("fall")).setForceLoopMode(BlueprintAnimation.LoopMode.HOLD);
        UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
            animationHandler.playAnimation("open", 0, 0, 1, true);
            Objects.requireNonNull(animationHandler.getAnimation("open")).setForceLoopMode(BlueprintAnimation.LoopMode.HOLD);
        }, fallDuration + 2L);

        // Play particle effects
        queueEffects(location, fallDuration + 2L);

        // Then spawn the items AFTER
        new BukkitRunnable() {
            double angle = 0;
            @Override
            public void run() {
                if (!iterator.hasNext()) {
                    this.cancel();
                    dummy.setRemoved(true);
                    new SoundEffect(Sound.ENTITY_PLAYER_TELEPORT, 0.4F, 1.2F).play(location);
                    Particle.POOF.builder()
                            .count(25)
                            .offset(0.2, 0.2, 0.2)
                            .extra(0.1)
                            .location(location)
                            .receivers(60)
                            .spawn();
                    return; // No more items
                }

                final Loot<?, ?> loot = iterator.next();
                if (loot instanceof DroppedItemLoot itemLoot) {
                    final Item item = itemLoot.award(context);
                    // Shoot it out in a circle
                    final double radians = Math.toRadians(angle);
                    angle += 22.5; // controls spacing and spiral motion
                    angle += (Math.random() * 6 - 3); // small chaotic variance
                    shootItem(radians, item);

                    // Prevent despawning
                    item.setPickupDelay(20); // 1-second pickup delay
                    item.setUnlimitedLifetime(true);
                    item.setCanMobPickup(false);
                    item.setThrower(player.getUniqueId());
                    item.setWillAge(false);
                    item.setInvulnerable(true);

                    // Sound
                    new SoundEffect(Sound.BLOCK_BEEHIVE_EXIT, 2f, 1f).play(location);
                } else {
                    loot.award(context);
                }
            }
        }.runTaskTimer(JavaPlugin.getPlugin(Core.class), openDuration + fallDuration + 20L, 1L);

        return true;
    }

    private void queueEffects(Location location, long jumpDelay) {
        Core plugin = JavaPlugin.getPlugin(Core.class);
        // Initial appearance
        new SoundEffect(Sound.ENTITY_PLAYER_TELEPORT, 1.4F, 1.2F).play(location);
        Particle.POOF.builder()
                .count(25)
                .offset(0.2, 0.2, 0.2)
                .extra(0.1)
                .location(location.clone().add(0, 3, 0))
                .receivers(60)
                .spawn();

        // Landing
        final Sound landSound = location.getBlock().getRelative(BlockFace.DOWN).getBlockData().getSoundGroup().getBreakSound();
        final SoundEffect landSFX = new SoundEffect(landSound, 1.4F, 1.2F);
        final ParticleBuilder landVFX = Particle.BLOCK_CRUMBLE.builder()
                .location(location)
                .offset(0.3, 0.3, 0.3)
                .receivers(60)
                .extra(0.2)
                .count(50)
                .data(Material.OAK_PLANKS.createBlockData())
                .spawn();
        UtilServer.runTaskLater(plugin, () -> {
            landSFX.play(location);
            landVFX.spawn();
        }, (long) (0.25 * 20L));

        // First jump
        final SoundEffect firstJumpSFX = new SoundEffect(Sound.BLOCK_BAMBOO_WOOD_STEP, 0F, 1.2F);
        UtilServer.runTaskLater(plugin, () -> {
            firstJumpSFX.play(location);
            landVFX.spawn();
        }, jumpDelay + (long) (0.29 * 20L));

        // Second jump
        final SoundEffect secondJumpSFX = new SoundEffect(Sound.BLOCK_BAMBOO_WOOD_STEP, 0F, 1.2F);
        UtilServer.runTaskLater(plugin, () -> {
            secondJumpSFX.play(location);
            landVFX.spawn();
        }, jumpDelay + (long) (1.88 * 20L));

        // Third jump
        final SoundEffect thirdJumpFX = new SoundEffect(Sound.BLOCK_BAMBOO_WOOD_STEP, 0F, 1.2F);
        UtilServer.runTaskLater(plugin, () -> {
            thirdJumpFX.play(location);
            landVFX.spawn();
        }, jumpDelay + (long) (3.25 * 20L));

        // Open FX
        final SoundEffect openSFX = new SoundEffect(Sound.BLOCK_ENDER_CHEST_OPEN, 1.3F, 1.2F);
        final ParticleBuilder openVFX = Particle.POOF.builder()
                .count(25)
                .offset(0.2, 0.2, 0.2)
                .extra(0.1)
                .location(location)
                .receivers(60);
        UtilServer.runTaskLater(plugin, () -> {
            openSFX.play(location);
            openVFX.spawn();
        }, jumpDelay + (long) (4.33 * 20L));
    }

    private void shootItem(double radians, Item item) {
        double radius = 0.6;

        // Direction vector (circle around player)
        double x = Math.cos(radians) * radius;
        double z = Math.sin(radians) * radius;

        // Slight vertical lift
        double y = 0.15 + (Math.random() * 0.05);

        // Set velocity (normalize for consistency)
        Vector velocity = new Vector(x, y, z).normalize().multiply(0.25);
        item.setVelocity(velocity);
    }
} 