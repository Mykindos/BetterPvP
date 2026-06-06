package me.mykindos.betterpvp.clans.world.veloran.gateway;

import com.google.inject.Provider;
import dev.brauw.mapper.region.CuboidRegion;
import me.mykindos.betterpvp.clans.world.veloran.Veloran;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.scene.behavior.SceneBehavior;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The Sundered Gate's entry trigger: each tick it detects players stepping <em>into</em> the gate cuboid and fires
 * {@link #onEnter(Player)} exactly once per entry.
 * <p>
 * Entry is edge-detected via {@link #inside}: a player already standing in the volume does not re-trigger every tick,
 * and only fires again after leaving and returning. The destination teleport itself is intentionally not wired yet —
 * {@link #onEnter(Player)} is the single seam where the Veloran teleport will live.
 */
public class GatewayTeleportBehavior implements SceneBehavior {

    private static final NamespacedKey ATTRIBUTE_KEY = new NamespacedKey("betterpvp", "veloran_teleport");

    private final ClientManager clientManager;
    private final Provider<Veloran> veloranProvider;
    private final CuboidRegion region;
    private final Set<UUID> inside = new HashSet<>();

    public GatewayTeleportBehavior(ClientManager clientManager, Provider<Veloran> veloranProvider, CuboidRegion region) {
        this.clientManager = clientManager;
        this.veloranProvider = veloranProvider;
        this.region = region;
    }

    @Override
    public void tick() {
        final World world = region.getWorld();
        if (world == null) {
            return;
        }

        final BoundingBox boundingBox = BoundingBox.of(
                region.getMin().toVector(),
                region.getMax().toVector()
        );
        boundingBox.expand(3, 0, 3);

        for (Player player : world.getPlayers()) {
            final boolean nowInside = boundingBox.contains(player.getLocation().toVector());
            final boolean wasInside = inside.contains(player.getUniqueId());

            if (nowInside && !wasInside) {
                inside.add(player.getUniqueId());
                onEnter(player);
            } else if (!nowInside && wasInside) {
                inside.remove(player.getUniqueId());
            }
        }

        // Drop players who disconnected or changed worlds while inside, so the set can't leak.
        inside.removeIf(id -> Bukkit.getPlayer(id) == null);
    }

    /**
     * Invoked once when {@code player} first steps into the gate volume.
     * <p>
     * Destination teleport wiring is deferred — this is the single seam where it will be added.
     *
     * @param player the player who just entered the gate
     */
    private void onEnter(Player player) {
        final Gamer gamer = clientManager.search().online(player).getGamer();
        final String continentName = veloranProvider.get().name();

        // cues
        showTitle(gamer, continentName);
        freeze(player);
        new SoundEffect("emewoods1", "custom.enchantedwoods1.cast_cue", 0.4f, 1f).play(player, player);

        // delayed teleport
        UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
            if (!player.isOnline()) {
                return;
            }
            unfreeze(player);
            teleport(player);
        }, 31L);
    }

    private static void teleport(Player player) {
        // todo: actually teleport to veloran
        Particle.CLOUD.builder()
                .count(20)
                .offset(1, 1, 1)
                .extra(0.3)
                .location(UtilPlayer.getMidpoint(player))
                .receivers(60)
                .spawn();
        player.teleport(player.getWorld().getSpawnLocation());
    }

    private static void showTitle(Gamer gamer, String continentName) {
        gamer.getTitleQueue().add(-Integer.MAX_VALUE, new TitleComponent(
                1.5,
                2.0,
                1.5,
                false,
                gmr -> Component.text("\ue800").font(Resources.Font.MENU),
                gmr -> Resources.Font3d.of(continentName)
        ));
    }

    private void freeze(Player player) {
        final AttributeInstance speedAttribute = Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED));
        final AttributeInstance jumpAttribute = Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH));
        speedAttribute.addTransientModifier(new AttributeModifier(ATTRIBUTE_KEY, -Integer.MAX_VALUE, AttributeModifier.Operation.ADD_NUMBER));
        jumpAttribute.addTransientModifier(new AttributeModifier(ATTRIBUTE_KEY, -Integer.MAX_VALUE, AttributeModifier.Operation.ADD_NUMBER));

        player.setVelocity(new Vector());
    }

    private void unfreeze(Player player) {
        final AttributeInstance speedAttribute = Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED));
        final AttributeInstance jumpAttribute = Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH));
        speedAttribute.removeModifier(ATTRIBUTE_KEY);
        jumpAttribute.removeModifier(ATTRIBUTE_KEY);
    }

    @Override
    public void stop() {
        inside.clear();
    }
}
