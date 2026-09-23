package me.mykindos.betterpvp.core.world.schematic.ghost;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The ghost each player is looking at, at most one each, taken away when they leave.
 * <p>
 * Whatever opens a ghost (a held blueprint, a builder command) decides where it goes and whether it fits. This only
 * makes sure a player never has two, and that none is left behind.
 */
@BPvPListener
@Singleton
public class GhostPreviews implements Listener {

    private final Core core;
    private final GhostMesher mesher = GhostMesher.standard();
    private final Map<UUID, GhostPreview> previews = new HashMap<>();

    @Inject
    public GhostPreviews(@NotNull Core core) {
        this.core = core;
    }

    /** Opens a ghost of {@code schematic} for {@code viewer}, closing any ghost they already had. Nothing shows until it is placed. */
    public @NotNull GhostPreview open(@NotNull Player viewer, @NotNull Schematic schematic) {
        close(viewer);
        final GhostPreview preview = new GhostPreview(core, viewer, schematic, mesher);
        previews.put(viewer.getUniqueId(), preview);
        return preview;
    }

    public @NotNull Optional<GhostPreview> of(@NotNull Player viewer) {
        return Optional.ofNullable(previews.get(viewer.getUniqueId()));
    }

    public void close(@NotNull Player viewer) {
        final GhostPreview preview = previews.remove(viewer.getUniqueId());
        if (preview != null) {
            preview.hide();
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        close(event.getPlayer());
    }
}
