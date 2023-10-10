package me.mykindos.betterpvp.core.framework.display;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class DisplayEditorManager implements Listener {

    private static final Object DUMMY = new Object();

    private static Component quaternion(String key, Quaternionf quaternion) {
        return UtilMessage.deserialize("<green>%s: <dark_green>[<aqua>%.2f</aqua>, <red>%.2fi</red>, <gold>%.2fj</gold>, <yellow>%.2fk</yellow>]",
                key,
                quaternion.w,
                quaternion.x,
                quaternion.y,
                quaternion.z
        );
    }

    private static Component vector(String key, Vector3f vector) {
        return UtilMessage.deserialize("<green>%s: <dark_green>[<gray>%.2f</gray>, <gray>%.2f</gray>, <gray>%.2f</gray>]",
                key,
                vector.x,
                vector.y,
                vector.z
        );
    }

    @Inject
    private GamerManager gamerManager;

    private final WeakHashMap<Player, Display> selectedDisplays = new WeakHashMap<>();
    private final LoadingCache<Player, Object> selecting = Caffeine.newBuilder()
            .weakKeys()
            .expireAfterWrite(15, java.util.concurrent.TimeUnit.SECONDS)
            .build(player -> DUMMY);

    private final PermanentComponent actionBar = new PermanentComponent(gmr -> {
        final Display display = selectedDisplays.get(gmr.getPlayer());
        final Transformation t = display.getTransformation();
        final Component translation = vector("Translation", t.getTranslation());
        final Component left = quaternion("Left", t.getLeftRotation());
        final Component scale = vector("Scale", t.getScale());
        final Component right = quaternion("Right", t.getRightRotation());
        return translation.appendSpace().append(left).appendSpace().append(scale).appendSpace().append(right);
    });

    /**
     * Get the selected display for a player
     * @param player The player
     * @param display The display
     * @return True if the display was selected or there was a display to deselect, false otherwise
     */
    public boolean selectDisplay(@NotNull Player player, @Nullable Display display) {
        final Display previous = selectedDisplays.remove(player);
        if (previous != null) {
            UtilPlayer.setGlowing(player, previous, false);
        }

        final Gamer gamer = gamerManager.getObject(player.getUniqueId()).orElseThrow();
        if (display == null) {
            gamer.getActionBar().remove(actionBar);
            return previous != null;
        }

        selectedDisplays.put(player, display);
        display.setGlowColorOverride(Color.GREEN);
        UtilPlayer.setGlowing(player, display, true);
        gamer.getActionBar().add(50, actionBar);
        return true;
    }

    public Display getSelectedDisplay(Player player) {
        return selectedDisplays.get(player);
    }

    public void startSelecting(@NotNull Player player) {
        selecting.refresh(player);
    }

    @EventHandler
    public void onInteract(PlayerArmSwingEvent event) {
        if (!selecting.asMap().containsKey(event.getPlayer())) {
            return;
        }

        final Vector direction = event.getPlayer().getLocation().getDirection();
        final Collection<Display> displays = event.getPlayer().getWorld().getEntitiesByClass(Display.class);
        Entity result = null;
        for (double i = 0; i < 3; i += 0.25) {
            final Location location = event.getPlayer().getEyeLocation().add(direction.clone().multiply(i));
            for (Display display : displays) {
                if (display.getLocation().distanceSquared(location) <= 1) {
                    result = display;
                    break;
                }
            }
        }

        if (!(result instanceof Display display)) {
            return;
        }

        selectDisplay(event.getPlayer(), display);
        selecting.invalidate(event.getPlayer());
        UtilMessage.simpleMessage(event.getPlayer(), "Display", "Selected display entity.");
    }

}
