package me.mykindos.betterpvp.clans.clans.map.events;

import me.mykindos.betterpvp.clans.clans.map.data.ExtraCursor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.map.MapCursorCollection;

import java.util.ArrayList;
import java.util.List;

public class MinimapExtraCursorEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private MapCursorCollection cursors;
    private int scale;
    private List<ExtraCursor> cursor = new ArrayList<>();

    public MinimapExtraCursorEvent(Player player, MapCursorCollection cursors, int scale) {
        this.player = player;
        this.cursors = cursors;
        this.scale = scale;
    }

    public int getScale() {
        return scale;
    }

    public MapCursorCollection getCursorCollection() {
        return cursors;
    }

    public List<ExtraCursor> getCursors() {
        return this.cursor;
    }

    public Player getPlayer() {
        return this.player;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}