package me.mykindos.betterpvp.core.combat.listeners.mythicmobs;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@PluginAdapter("ModelEngine")
@PluginAdapter("MythicMobs")
@PluginAdapter("ProtocolLib")
@BPvPListener
@Singleton
public class DamageIndicatorAdapter implements Listener {

    private final Multimap<UUID, DamageIndicator> indicators = ArrayListMultimap.create();

    @Inject
    private Core core;

    @Inject
    @Config(path = "pvp.mythic-damage-indicators.enabled", defaultValue = "true")
    private boolean enabled;

    @Inject
    @Config(path = "pvp.mythic-damage-indicators.scale", defaultValue = "3.0")
    private double scale;

    @Inject
    @Config(path = "pvp.mythic-damage-indicators.duration", defaultValue = "1.5")
    private double duration;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(final CustomDamageEvent event) {
        if (!enabled || !(event.getDamager() instanceof Player player)) {
            return;
        }

        final Optional<ActiveMob> mobOpt = MythicBukkit.inst().getMobManager().getActiveMob(event.getDamagee().getUniqueId());
        if (mobOpt.isEmpty()) {
            return; // Only add damage indicators for mythic mobs
        }

        final Vector direction = player.getEyeLocation().getDirection();
        final Location spawnPoint;
        if (event.getProjectile() != null) {
            spawnPoint = event.getProjectile().getLocation();
        } else {
            spawnPoint = event.getDamagee().getEyeLocation().toVector()
                    .subtract(player.getEyeLocation().toVector())
                    .multiply(0.7)
                    .toLocation(player.getWorld())
                    .add(event.getDamager().getEyeLocation());
            spawnPoint.add(
                    Math.random() * 0.5 - 0.5,
                    Math.random() * 0.5 - 0.5,
                    Math.random() * 0.5 - 0.5
            );
        }

        final Component text = formatDamage(event.getDamage());
        final TextDisplay shadow = spawn(spawnPoint, player);
        final Location shadowLocation = spawnPoint.subtract(direction.multiply(0.01).add(new Vector(0, -0.02, 0)));
        final TextDisplay display = spawn(shadowLocation, player);
        display.text(text.color(NamedTextColor.RED));
        shadow.text(text.color(NamedTextColor.DARK_RED));

        indicators.put(player.getUniqueId(), new DamageIndicator(display,
                shadow,
                event.getDamage(),
                System.currentTimeMillis()));
    }

    private Component formatDamage(double damage) {
        return Component.text("\u2764" + UtilFormat.formatNumber(damage, 1, false));
    }

    @SuppressWarnings("UnstableApiUsage")
    private TextDisplay spawn(Location location, Player player) {
        final TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, ent -> {
            ent.setBackgroundColor(Color.fromARGB(0, 1, 1, 1));
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setAlignment(TextDisplay.TextAlignment.CENTER);
            ent.setBillboard(Display.Billboard.CENTER);
            ent.setPersistent(false);
            ent.setShadowed(false);
            ent.setSeeThrough(false);
            ent.setVisibleByDefault(false);
            ent.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f((float) scale, (float) scale, (float) scale),
                    new AxisAngle4f()
            ));
            player.showEntity(core, ent);
        });
        UtilEntity.setViewRangeBlocks(display, 100);
        return display;
    }

    @UpdateEvent
    public void onUpdate() {
        if (!enabled) {
            return;
        }

        final long currentTime = System.currentTimeMillis();
        final Iterator<Map.Entry<UUID, DamageIndicator>> iterator = indicators.entries().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, DamageIndicator> entry = iterator.next();
            final Player player = Bukkit.getPlayer(entry.getKey());
            final DamageIndicator indicator = entry.getValue();
            if (!indicator.isValid()) {
                iterator.remove();
                continue;
            }

            // Remove if the indicator has expired or the player logged off
            final float timePassed = (currentTime - indicator.timestamp());
            if (player == null || !player.isOnline() || timePassed > (duration * 1000)) {
                indicator.despawn();
                iterator.remove();
                continue;
            }

            // Update the damage indicator
            final float progress = (float) (1 - timePassed / (duration * 1000));
            final Transformation transformation = new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f((float) scale * progress, (float) scale * progress, (float) scale * progress),
                    new AxisAngle4f()
            );
            final byte opacity = (byte) Math.max(255 * progress, 20);

            indicator.display().setTextOpacity(opacity);
            indicator.display().setTransformation(transformation);
            indicator.shadow().setTextOpacity(opacity);
            indicator.shadow().setTransformation(transformation);
        }
    }

}
