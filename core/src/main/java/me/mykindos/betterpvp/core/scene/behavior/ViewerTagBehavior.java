package me.mykindos.betterpvp.core.scene.behavior;

import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A label above a scene object that only some players can see, written in each of their own languages.
 * <p>
 * One display per viewer rather than one shared display, because both halves of that need it: a shared entity carries a
 * single text, which cannot be two languages at once, and hiding one entity from most of the server is a packet per
 * player anyway. Each display is spawned invisible to everybody and then shown to the one player it was written for.
 * <p>
 * Who sees it is re-asked on a beat rather than pushed at this behaviour, so a filter reading changing state — whether
 * this viewer is a captain with a ship waiting, say — keeps up without anything having to tell the label about it.
 *
 * @see TagBehavior for the shared, everybody-sees-it counterpart
 */
public class ViewerTagBehavior implements SceneBehavior {

    /** Beyond this nobody is offered the label, whatever the filter says. */
    private static final double VIEW = 32.0;
    private static final int REFRESH_TICKS = 20;

    private final Plugin plugin;
    private final TagAnchor anchor;
    private final Vector offset;
    private final Predicate<Player> audience;
    private final Component text;

    private final Map<UUID, TextDisplay> displays = new HashMap<>();
    private int countdown;

    /**
     * Anchors the label 0.2 blocks above the top of the owner's bounding box.
     *
     * @param audience who this is worth showing to; re-asked roughly once a second
     * @param text     what it says, rendered into each viewer's own locale
     */
    public ViewerTagBehavior(@NotNull SceneObject owner, @NotNull Plugin plugin, @NotNull Predicate<Player> audience,
                             @NotNull Component text) {
        this(plugin, TagAnchor.aboveEntity(owner), new Vector(), audience, text);
    }

    public ViewerTagBehavior(@NotNull Plugin plugin, @NotNull TagAnchor anchor, @NotNull Vector offset,
                             @NotNull Predicate<Player> audience, @NotNull Component text) {
        this.plugin = plugin;
        this.anchor = anchor;
        this.offset = offset.clone();
        this.audience = audience;
        this.text = text;
    }

    @Override
    public void tick() {
        if (--countdown > 0) {
            return;
        }
        countdown = REFRESH_TICKS;

        final Location at = anchor.resolve();
        if (at == null) {
            stop();
            return;
        }
        at.add(offset);

        displays.entrySet().removeIf(entry -> {
            final Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer != null && shouldSee(viewer, at) && !UtilEntity.isRemoved(entry.getValue())) {
                return false;
            }
            despawn(entry.getValue());
            return true;
        });

        for (Player viewer : at.getWorld().getPlayers()) {
            if (!displays.containsKey(viewer.getUniqueId()) && shouldSee(viewer, at)) {
                displays.put(viewer.getUniqueId(), spawn(viewer, at));
            }
        }
    }

    @Override
    public void stop() {
        displays.values().forEach(this::despawn);
        displays.clear();
    }

    private boolean shouldSee(@NotNull Player viewer, @NotNull Location at) {
        return viewer.getWorld().equals(at.getWorld())
                && viewer.getLocation().distanceSquared(at) <= VIEW * VIEW
                && audience.test(viewer);
    }

    /**
     * Spawns this viewer's own copy. Invisible by default and then revealed to them alone, so it is never sent to
     * anybody else — the reverse of hiding it from everyone, which would leak it to whoever logs in next.
     */
    private @NotNull TextDisplay spawn(@NotNull Player viewer, @NotNull Location at) {
        final TextDisplay display = at.getWorld().spawn(at, TextDisplay.class, spawned -> {
            spawned.setVisibleByDefault(false);
            spawned.setPersistent(false);
            spawned.setBillboard(Display.Billboard.CENTER);
            spawned.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            spawned.setShadowed(true);
            spawned.setSeeThrough(true);
            spawned.text(Translations.render(text, viewer.locale()));
        });
        viewer.showEntity(plugin, display);
        return display;
    }

    private void despawn(@NotNull TextDisplay display) {
        if (!UtilEntity.isRemoved(display)) {
            display.remove();
        }
    }
}
