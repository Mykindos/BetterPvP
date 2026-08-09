package me.mykindos.betterpvp.clans.clans.map.cursor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.map.data.PlayerMark;
import me.mykindos.betterpvp.core.map.PointOfInterest;
import me.mykindos.betterpvp.core.map.events.MapPointOfInterestEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures, once per tick, everything the cursor layer needs about the server: where every player is, which way they
 * face and which clan they belong to. Viewers then filter that snapshot instead of sweeping the player list themselves.
 * <p>
 * Points of interest are refreshed on a much slower cadence, since Mapper-authored icons and dynamic providers do not
 * move between ticks.
 */
@Singleton
public class MapCursorService {

    private static final long POI_REFRESH_TICKS = 20L * 5;

    private final ClanManager clanManager;

    private volatile List<PlayerMark> marks = List.of();
    private volatile List<PointOfInterest> pointsOfInterest = List.of();

    @Inject
    public MapCursorService(Clans clans, ClanManager clanManager) {
        this.clanManager = clanManager;

        UtilServer.runTaskTimer(clans, this::captureMarks, 1L, 1L);
        UtilServer.runTaskTimer(clans, this::refreshPointsOfInterest, POI_REFRESH_TICKS, POI_REFRESH_TICKS);
        refreshPointsOfInterest();
    }

    /**
     * @return this tick's player snapshot; safe to iterate from any thread and never mutated in place
     */
    public @NotNull List<PlayerMark> getMarks() {
        return marks;
    }

    public @NotNull List<PointOfInterest> getPointsOfInterest() {
        return pointsOfInterest;
    }

    private void captureMarks() {
        final List<PlayerMark> captured = new ArrayList<>(Bukkit.getOnlinePlayers().size());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead()) {
                continue;
            }
            final Location location = player.getLocation();
            final Clan clan = clanManager.getClanByPlayer(player).orElse(null);
            captured.add(new PlayerMark(player.getUniqueId(), player.getName(),
                    location.getBlockX(), location.getBlockZ(), player.getWorld().getName(),
                    direction(location.getYaw()), clan == null ? null : clan.getId()));
        }
        marks = List.copyOf(captured);
    }

    private static byte direction(float yaw) {
        float normalised = yaw;
        if (normalised < 0.0F) {
            normalised += 360.0F;
        }
        final int direction = (int) ((Math.abs(normalised) + 11.25D) / 22.5D);
        return (byte) (direction > 15 ? 0 : direction);
    }

    private void refreshPointsOfInterest() {
        final MapPointOfInterestEvent event = UtilServer.callEvent(new MapPointOfInterestEvent());
        pointsOfInterest = List.copyOf(event.getPointsOfInterest());
    }
}
