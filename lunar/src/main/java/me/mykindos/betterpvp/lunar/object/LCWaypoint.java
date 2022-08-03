package me.mykindos.betterpvp.lunar.object;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.lunar.LunarClientAPI;
import org.bukkit.Location;

@Data
@AllArgsConstructor
public final class LCWaypoint {

    private final String name;
    private final int x;
    private final int y;
    private final int z;
    private final String world;
    private final int color;
    private final boolean forced;
    private final boolean visible;

    /**
     * Create a waypoint with any visibility.
     *
     * NOTE: Just because a waypoint object is created, that doesn't mean
     * it will be sent to users. To display the waypoint you must still send
     * it to the Lunar Client user.
     *
     * @param name The name of the waypoint (this is important if you need to edit it later, save the name).
     * @param location The bukkit {@link Location} to display the waypoint at. Whole blocks only, partial blocks aren't accounted for.
     * @param color The HEX color to set as the color for the waypoint.
     * @param forced If the client should be able to remove the waypoint, or if it is forced for gameplay reasons.
     * @param visible Weather or not the waypoint should be visible. This will likely always be true.
     */
    public LCWaypoint(String name, Location location, int color, boolean forced, boolean visible) {
        this(name, location.getBlockX(), location.getBlockY(), location.getBlockZ(), LunarClientAPI.getInstance().getWorldIdentifier(location.getWorld()), color, forced, visible);
    }

    /**
     * Create a waypoint that is automatically visible when sent.
     *
     * NOTE: Just because a waypoint object is created, that doesn't mean
     * it will be sent to users. To display the waypoint you must still send
     * it to the Lunar Client user.
     *
     * @param name The name of the waypoint (this is important if you need to edit it later, save the name).
     * @param location The bukkit {@link Location} to display the waypoint at. Whole blocks only, partial blocks aren't accounted for.
     * @param color The HEX color to set as the color for the waypoint.
     * @param forced If the client should be able to remove the waypoint, or if it is forced for gameplay reasons.
     */
    public LCWaypoint(String name, Location location, int color, boolean forced) {
        this(name, location, color, forced, true);
    }

}