package me.mykindos.betterpvp.game.impl.domination.model;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.module.powerup.Powerup;
import me.mykindos.betterpvp.game.impl.domination.Domination;
import me.mykindos.betterpvp.game.impl.domination.controller.GameController;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.SMALL_CAPS;

@CustomLog
public class Gem implements Powerup {

    private final Location location;
    private final GamePlugin plugin;
    private final GameController controller;
    private final Domination game;
    private Display display;
    private boolean enabled;
    private BukkitTask cooldownTask;

    public Gem(Location location, GamePlugin plugin, GameController controller, Domination domination) {
        this.location = location.clone();
        this.plugin = plugin;
        this.controller = controller;
        this.game = domination;
        this.enabled = true;
    }

    @Override
    public void setup() {
        // No additional setup needed
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public void activate() {
        // Set block to emerald
        location.getBlock().getRelative(BlockFace.DOWN).setType(Material.EMERALD_BLOCK);

        // Despawn the item
        if (display != null) {
            display.remove();
            display = null;
        }
        
        // Spawn the item
        display = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setPersistent(false);
            spawned.setItemStack(new org.bukkit.inventory.ItemStack(Material.EMERALD));
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
        final float oscillationFactor = (float) (Math.sin(time / 1000d) * 0.2);
        final float angle = (float) ((time / 20d) % 360);

        final Vector3f scale = new Vector3f(1.5f);
        final AxisAngle4f rotation = new AxisAngle4f((float) Math.toRadians(angle), 0, 1, 0);
        final Vector3f translation = new Vector3f(0, 1.0f, 0).add(0, oscillationFactor, 0);
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
        if (Bukkit.getCurrentTick() % 5 == 0) {
            Particle.HAPPY_VILLAGER.builder()
                    .location(location.clone().add(0, translation.y, 0))
                    .receivers(60)
                    .offset(0.7, 0.7, 0.7)
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
        // Award points to the team
        final Team team = Objects.requireNonNull(game.getPlayerTeam(player));
        int score = game.getConfiguration().getGemScoreAttribute().getValue();
        controller.addPoints(team, score);

        // Put the gem on cooldown
        startCooldown();

        // Sound Cue
        final SoundEffect sound = new SoundEffect("betterpvp", "game.domination.gem_pickup");
        for (Player teamPlayer : team.getPlayers()) {
            sound.play(teamPlayer);
        }

        // Chat Cue
        UtilMessage.message(player, Component.newline()
                .append(Component.text("You scored  ", NamedTextColor.GREEN, TextDecoration.BOLD).font(SMALL_CAPS))
                .append(Component.text("+" + score, NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("  points for your team!", NamedTextColor.GREEN, TextDecoration.BOLD).font(SMALL_CAPS))
                .appendNewline());

        // Title Cue
        for (Participant teammate : team.getParticipants()) {
            final Gamer gamer = teammate.getClient().getGamer();
            gamer.getTitleQueue().add(2, TitleComponent.subtitle(0, 2.5, 0.6, false, gmr -> {
                if (gmr.getPlayer() == player) {
                    return Component.text("+" + score, NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text(" Points", NamedTextColor.GREEN, TextDecoration.BOLD).font(SMALL_CAPS));
                } else {
                    return Component.text(player.getName(), NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text("  picked up  ", NamedTextColor.GREEN, TextDecoration.BOLD).font(SMALL_CAPS))
                            .append(Component.text(score, NamedTextColor.GREEN, TextDecoration.BOLD))
                            .append(Component.text("  Points", NamedTextColor.GREEN, TextDecoration.BOLD).font(SMALL_CAPS));
                }
            }));
        }
    }

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
        spawnFirework();

        // Schedule reactivation
        if (cooldownTask != null) {
            cooldownTask.cancel();
        }

        // 60 seconds
        cooldownTask = UtilServer.runTaskLater(plugin, this::activate, 60 * 20L);
    }

    private void spawnFirework() {
        Firework firework = location.getWorld().spawn(location.clone().add(0, 0.5, 0), Firework.class);
        final FireworkMeta meta = firework.getFireworkMeta();
        meta.clearEffects();
        meta.setPower(1);
        final FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.LIME)
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