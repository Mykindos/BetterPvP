package me.mykindos.betterpvp.clans.clans.map.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.map.data.ExtraCursor;
import me.mykindos.betterpvp.clans.clans.map.events.MinimapExtraCursorEvent;
import me.mykindos.betterpvp.clans.clans.map.events.MinimapPlayerCursorEvent;
import me.mykindos.betterpvp.clans.clans.pillage.PillageHandler;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.map.MapCursor;

import java.util.HashMap;
import java.util.Objects;


@BPvPListener
public class MapCursorListener implements Listener {

    private final Clans clans;
    private final ClientManager clientManager;
    private final ClanManager clanManager;
    private final PillageHandler pillageHandler;

    private final HashMap<String, Clan> clanCache = new HashMap<>();

    @Inject
    @Config(path = "clans.map.player-captions", defaultValue = "true")
    private boolean playerCaptions;

    @Inject
    @Config(path = "clans.map.location-captions", defaultValue = "true")
    private boolean locationCaptions;


    @Inject
    public MapCursorListener(Clans clans, ClientManager clientManager, ClanManager clanManager, PillageHandler pillageHandler) {
        this.clans = clans;
        this.clientManager = clientManager;
        this.clanManager = clanManager;
        this.pillageHandler = pillageHandler;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCursor(MinimapExtraCursorEvent event) {
        Player player = event.getPlayer();
        Clan aClan = clanManager.getClanByPlayer(player).orElse(null);

        Client client = clientManager.search().online(player);

        if(locationCaptions && (boolean) client.getProperty(ClientProperty.MAP_POINTS_OF_INTEREST).orElse(false)) {
            adminClanLocations(event);
        }

        boolean playerNames = (boolean) client.getProperty(ClientProperty.MAP_PLAYER_NAMES).orElse(false);

        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            if (otherPlayer.isDead()) continue;
            if (otherPlayer.getWorld().equals(player.getWorld())) {
                float yaw = otherPlayer.getLocation().getYaw();
                if (yaw < 0.0F) {
                    yaw += 360.0F;
                }
                byte direction = (byte) (int) ((Math.abs(yaw) + 11.25D) / 22.5D);
                if (direction > 15) {
                    direction = 0;
                }
                int x = otherPlayer.getLocation().getBlockX();
                int z = otherPlayer.getLocation().getBlockZ();

                MinimapPlayerCursorEvent cursorEvent = null;
                if (client.isAdministrating()) {
                    cursorEvent = new MinimapPlayerCursorEvent(player, otherPlayer, true, MapCursor.Type.PLAYER, null);
                } else {
                    Clan bClan = clanManager.getClanByPlayer(otherPlayer).orElse(null);

                    if (aClan == null) {
                        if (player == otherPlayer) {
                            cursorEvent = new MinimapPlayerCursorEvent(player, otherPlayer, true, MapCursor.Type.PLAYER, null);
                        }
                    } else {
                        if (bClan != null) {
                            if (aClan == bClan) {
                                if (player == otherPlayer) {
                                    cursorEvent = new MinimapPlayerCursorEvent(player, otherPlayer, true, MapCursor.Type.PLAYER, null);
                                } else {
                                    cursorEvent = new MinimapPlayerCursorEvent(player, otherPlayer, true, MapCursor.Type.BLUE_MARKER, (playerCaptions && playerNames) ? otherPlayer.getName() : null);
                                }
                            } else if (aClan.isAllied(bClan)) {
                                cursorEvent = new MinimapPlayerCursorEvent(player, otherPlayer, true, MapCursor.Type.FRAME,  (playerCaptions && playerNames) ? otherPlayer.getName() : null);
                            } else if (pillageHandler.isPillaging(aClan, bClan) || pillageHandler.isPillaging(bClan, aClan)) {
                                cursorEvent = new MinimapPlayerCursorEvent(player, otherPlayer, true, MapCursor.Type.RED_MARKER, null);
                            }
                        }
                    }
                }
                if (cursorEvent != null) {
                    Bukkit.getPluginManager().callEvent(cursorEvent);
                    event.getCursors().add(new ExtraCursor(x, z, (player == otherPlayer) || (cursorEvent.isDisplay()),
                            cursorEvent.getType(), direction, otherPlayer.getWorld().getName(), true, cursorEvent.getCaption()));
                }
            }
        }

        if (aClan != null && aClan.getCore().isSet()) {
            Location coreLoc = Objects.requireNonNull(aClan.getCore().getPosition());
            event.getCursors().add(new ExtraCursor(coreLoc.getBlockX(), coreLoc.getBlockZ(), true,
                    MapCursor.Type.MANSION, (byte) 8, player.getWorld().getName(), true, null));
        }
    }

    private void adminClanLocations(MinimapExtraCursorEvent event) {
        addAdminClan(event, "Fields", MapCursor.Type.RED_MARKER, "Fields");
        addAdminClan(event, "Red Shops", MapCursor.Type.BANNER_RED, "Red Shops");
        addAdminClan(event, "Blue Shops", MapCursor.Type.BANNER_BLUE, "Blue Shops");
        addAdminClan(event, "Green Shops", MapCursor.Type.BANNER_LIME, "Green Shops");
        addAdminClan(event, "Yellow Shops", MapCursor.Type.BANNER_YELLOW, "Yellow Shops");
    }

    private void addAdminClan(MinimapExtraCursorEvent event, String clanName, MapCursor.Type cursor, String caption) {
        Clan clan = clanCache.computeIfAbsent(clanName, c -> clanManager.getClanByName(clanName).orElse(null));
        if (clan != null && clan.getCore().isSet()) {
            Location loc = Objects.requireNonNull(clan.getCore().getPosition());
            event.getCursors().add(new ExtraCursor(loc.getBlockX(), loc.getBlockZ(), true,
                    cursor, (byte) 8, loc.getWorld().getName(), true, caption));
        }
    }
}
