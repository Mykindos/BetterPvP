package me.mykindos.betterpvp.lunar.event;

import lombok.Getter;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public final class LCPacketSentEvent extends PlayerEvent {

    @Getter private static HandlerList handlerList = new HandlerList();

    @Getter private final LCPacket packet;

    /**
     * Called when the server sends a {@link LCPacket} to the Lunar Client user.
     * There is a chance the player does not receive the packet even if this event is
     * called. There is no way to be 100% certain the client has received and registered the packet.
     *
     * @param player The player receiving the incoming packet.
     * @param packet The {@link LCPacket} the server has sent to the Lunar Client user.
     */
    public LCPacketSentEvent(Player player, LCPacket packet) {
        super(player);
        this.packet = packet;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

}