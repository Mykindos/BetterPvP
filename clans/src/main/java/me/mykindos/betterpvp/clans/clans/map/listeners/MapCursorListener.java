package me.mykindos.betterpvp.clans.clans.map.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.map.data.ExtraCursor;
import me.mykindos.betterpvp.clans.clans.map.events.MinimapExtraCursorEvent;
import me.mykindos.betterpvp.clans.clans.map.events.MinimapPlayerCursorEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.map.MapCursor;


@BPvPListener
public class MapCursorListener implements Listener {

    private final Clans clans;
    private final ClanManager clanManager;

    @Inject
    public MapCursorListener(Clans clans, ClanManager clanManager) {
        this.clans = clans;
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCursor(MinimapExtraCursorEvent event) {
        Player player = event.getPlayer();
        Clan aClan = clanManager.getClanByPlayer(player).orElse(null);

        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
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


                Clan bClan = clanManager.getClanByPlayer(otherPlayer).orElse(null);

                MinimapPlayerCursorEvent cursorEvent = null;
                if (aClan == null) {
                    if (player == otherPlayer) {
                        cursorEvent = new MinimapPlayerCursorEvent(player, otherPlayer, true, MapCursor.Type.WHITE_POINTER);
                    }
                } else {
                    if (bClan != null) {
                        if (aClan == bClan) {
                            cursorEvent = new MinimapPlayerCursorEvent(player, otherPlayer, true, MapCursor.Type.BLUE_POINTER);
                        } else if (aClan.isAllied(bClan)) {
                            cursorEvent = new MinimapPlayerCursorEvent(player, otherPlayer, true, MapCursor.Type.GREEN_POINTER);
                        }
                    }
                }
                if (cursorEvent != null) {
                    Bukkit.getPluginManager().callEvent(cursorEvent);
                    event.getCursors().add(new ExtraCursor(x, z, (player == otherPlayer) || (cursorEvent.isDisplay()),
                            cursorEvent.getType(), direction, otherPlayer.getWorld().getName(), false));
                }
            }
        }
    }
}
