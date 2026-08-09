package me.mykindos.betterpvp.clans.clans.map.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.map.ClanMapService;
import me.mykindos.betterpvp.clans.clans.map.cursor.MapCursorService;
import me.mykindos.betterpvp.clans.clans.map.data.ExtraCursor;
import me.mykindos.betterpvp.clans.clans.map.data.PlayerMark;
import me.mykindos.betterpvp.clans.clans.map.events.MinimapExtraCursorEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.map.MapCursor;

import java.util.Objects;

/**
 * Places player and clan-core cursors for one viewer, reading the per-tick snapshot in {@link MapCursorService} and a
 * cached relation table.
 * <p>
 * Both are shared across viewers, so a viewer's cursor pass is arithmetic over already-captured data: no clan lookup
 * and no event dispatch per player observed, and therefore no cost that grows with the square of the player count.
 */
@BPvPListener
@Singleton
public class MapCursorListener implements Listener {

    private final ClientManager clientManager;
    private final ClanManager clanManager;
    private final ClanMapService clanMapService;
    private final MapCursorService cursorService;

    @Inject
    @Config(path = "clans.map.player-captions", defaultValue = "true")
    private boolean playerCaptions;

    @Inject
    public MapCursorListener(ClientManager clientManager, ClanManager clanManager, ClanMapService clanMapService,
                             MapCursorService cursorService) {
        this.clientManager = clientManager;
        this.clanManager = clanManager;
        this.clanMapService = clanMapService;
        this.cursorService = cursorService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCursor(MinimapExtraCursorEvent event) {
        final Player viewer = event.getPlayer();
        final Client client = clientManager.search().online(viewer);
        final boolean administrating = client.isAdministrating();
        final boolean captions = playerCaptions
                && (boolean) client.getProperty(ClientProperty.MAP_PLAYER_NAMES).orElse(false);

        final Clan viewerClan = clanManager.getClanByPlayer(viewer).orElse(null);
        final Long2ObjectMap<ClanRelation> relations = clanMapService.relations(viewer);
        final String world = viewer.getWorld().getName();

        for (PlayerMark mark : cursorService.getMarks()) {
            if (!mark.getWorld().equals(world)) {
                continue;
            }

            if (mark.getUuid().equals(viewer.getUniqueId())) {
                event.getCursors().add(cursor(mark, MapCursor.Type.PLAYER, null));
                continue;
            }
            if (administrating) {
                event.getCursors().add(cursor(mark, MapCursor.Type.PLAYER, null));
                continue;
            }
            if (viewerClan == null || mark.getClanId() == null) {
                continue;
            }

            final ClanRelation relation = relations.get((long) mark.getClanId());
            if (relation == null) {
                continue;
            }
            switch (relation) {
                case SELF -> event.getCursors()
                        .add(cursor(mark, MapCursor.Type.BLUE_MARKER, captions ? mark.getName() : null));
                case ALLY, ALLY_TRUST -> event.getCursors()
                        .add(cursor(mark, MapCursor.Type.FRAME, captions ? mark.getName() : null));
                case PILLAGE -> event.getCursors().add(cursor(mark, MapCursor.Type.RED_MARKER, null));
                default -> {
                    // Enemies and neutrals stay hidden, as before.
                }
            }
        }

        if (viewerClan != null && viewerClan.getCore().isSet()) {
            final Location core = Objects.requireNonNull(viewerClan.getCore().getPosition());
            event.getCursors().add(new ExtraCursor(core.getBlockX(), core.getBlockZ(), true,
                    MapCursor.Type.MANSION, (byte) 8, world, true, null));
        }
    }

    private ExtraCursor cursor(PlayerMark mark, MapCursor.Type type, String caption) {
        return new ExtraCursor(mark.getX(), mark.getZ(), true, type, mark.getDirection(),
                mark.getWorld(), true, caption);
    }
}
