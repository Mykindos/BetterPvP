package me.mykindos.betterpvp.clans.clans.map.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCursor;

@EqualsAndHashCode(callSuper = true)
@Data
public class MinimapPlayerCursorEvent extends CustomEvent {

    private Player viewer;
    private Player viewed;
    private MapCursor.Type type;
    private boolean display;

    public MinimapPlayerCursorEvent(Player viewer, Player viewed, boolean canSee, MapCursor.Type type) {
        this.viewed = viewed;
        this.viewer = viewer;
        this.display = canSee;
        this.type = type;
    }



}