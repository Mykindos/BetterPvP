package me.mykindos.betterpvp.game.framework.module.powerup.impl;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameMapStat;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import me.mykindos.betterpvp.game.framework.model.stats.StatManager;
import me.mykindos.betterpvp.game.framework.module.powerup.Powerup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.SMALL_CAPS;

/**
 * Represents a restock point where players can refresh their inventory
 */
@CustomLog
public class Restock implements Powerup {

    private final Location location;
    private final GamePlugin plugin;
    private final HotBarLayoutManager layoutManager;
    private final ClientManager clientManager;
    private Display display;
    private boolean enabled;
    private BukkitTask cooldownTask;
    private final StatManager statManager;

    public Restock(Location location, GamePlugin plugin, HotBarLayoutManager layoutManager, ClientManager clientManager, StatManager statManager) {
        this.location = location.clone();
        this.plugin = plugin;
        this.layoutManager = layoutManager;
        this.clientManager = clientManager;
        this.statManager = statManager;
        this.enabled = true;
    }

    @Override
    public void setup() {
        // ignore
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    /**
     * Activates this restock point by setting it to a gold block with a chest item
     */
    public void activate() {
        // Set block to gold
        location.getBlock().getRelative(BlockFace.DOWN).setType(Material.GOLD_BLOCK);

        // Despawn the item
        if (display != null) {
            display.remove();
            display = null;
        }
        
        // Spawn the item
        display = location.getWorld().spawn(location, BlockDisplay.class, spawned -> {
            spawned.setPersistent(false);
            spawned.setBlock(Material.CHEST.createBlockData());
            spawned.setBillboard(Display.Billboard.FIXED);
        });

        // Visual cues
        spawnFirework();

        enabled = true;
    }

    @Override
    public void tick() {
        if (!isEnabled()) {
            return;
        }
        Preconditions.checkNotNull(display);

        // Rotate the display
        final long time = System.currentTimeMillis();
        final float oscillationFactor = (float) (Math.sin(time / 1000d) * 0.3);
        final float angle = (float) ((time / 20d) % 360);

        final Vector3f scale = new Vector3f(0.8f);
        final AxisAngle4f rotation = new AxisAngle4f((float) Math.toRadians(angle), 0, 1, 0);
        final Vector3f translation = new Vector3f(-scale.x / 2, 0.32f, -scale.z / 2).add(0, oscillationFactor, 0);
        final Matrix4f translationMatrix = new Matrix4f();
        translationMatrix.setTranslation(translation);
        final Matrix4f transformationMatrix = new Matrix4f();
        transformationMatrix.rotate(rotation);
        transformationMatrix.mul(translationMatrix);
        transformationMatrix.scale(scale);

        display.setInterpolationDuration(1);
        display.setInterpolationDelay(0);
        display.setTransformationMatrix(transformationMatrix);

        // Particle cues
        if (Bukkit.getCurrentTick() % 3 == 0) {
            Particle.FIREWORK.builder()
                    .location(location.clone().add(0, translation.y, 0))
                    .receivers(60)
                    .extra(0.05)
                    .spawn();
        }
    }

    @Override
    public void tearDown() {
        // Clean up entities
        if (display != null) {
            display.remove();
            display = null;
        }

        // Restore block
        location.getBlock().getRelative(BlockFace.DOWN).setType(Material.IRON_BLOCK);

        // Cancel tasks
        if (cooldownTask != null) {
            cooldownTask.cancel();
            cooldownTask = null;
        }
    }

    @Override
    public void use(Player player) {
        // Get player's current build from HotBarLayoutManager and apply it
        try {
            // Apply the player's saved hotbar layout
            layoutManager.applyPlayerLayout(player);

            // Sound
            new SoundEffect("betterpvp", "game.generic.powerup.restock").play(player);

            // Chat
            final TextColor color = TextColor.color(232, 237, 74);
            UtilMessage.message(player, Component.newline()
                    .append(Component.text("Your inventory was restocked!", color, TextDecoration.BOLD).font(SMALL_CAPS))
                    .appendNewline());

            // Title
            final Gamer gamer = clientManager.search().online(player).getGamer();
            gamer.getTitleQueue().add(2, TitleComponent.subtitle(0, 2.5, 0.6, false, gmr -> {
                return Component.text("+ Restock", color, TextDecoration.BOLD).font(SMALL_CAPS);
            }));

            // Put the restock point on cooldown
            startCooldown();

            final GameMapStat.GameMapStatBuilder<?, ?> builder = GameMapStat.builder()
                    .action(GameMapStat.Action.RESTOCK);
            statManager.incrementGameStat(player.getUniqueId(), builder, 1);

        } catch (Exception e) {
            log.error("Failed to restock player " + player.getName(), e).submit();
        }
    }

    /**
     * Puts this restock point on cooldown
     */
    private void startCooldown() {
        enabled = false;

        // Remove the pickup item
        if (display != null) {
            display.remove();
            display = null;
        }

        // Change block to iron
        location.getBlock().getRelative(BlockFace.DOWN).setType(Material.IRON_BLOCK);

        // Visual cues
        // Cues
        spawnFirework();

        // Schedule reactivation
        if (cooldownTask != null) {
            cooldownTask.cancel();
        }

        // 60 seconds
        cooldownTask = UtilServer.runTaskLater(plugin, this::activate, 60 * 20L);
    }

    private void spawnFirework() {
        Firework firework = location.getWorld().spawn(location.clone().add(0, 0.5, 0) , Firework.class);
        final FireworkMeta meta = firework.getFireworkMeta();
        meta.clearEffects();
        meta.setPower(1);
        final FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.YELLOW)
                .flicker(false)
                .trail(false)
                .with(FireworkEffect.Type.BURST)
                .build();
        meta.addEffect(effect);
        firework.setFireworkMeta(meta);
        firework.getPersistentDataContainer().set(CoreNamespaceKeys.NO_DAMAGE, PersistentDataType.BOOLEAN, true);
        firework.detonate();
    }
}