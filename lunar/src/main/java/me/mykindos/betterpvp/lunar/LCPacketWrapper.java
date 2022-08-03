package me.mykindos.betterpvp.lunar;

import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import org.bukkit.entity.Player;


public interface LCPacketWrapper<T extends LCPacket> {

    /**
     * The packet that will be sent to the player
     * that is formed in the wrapper implementation.
     *
     * @return A LCPacket to send to the Lunar Client player
     */
    T getPacket();

    /**
     * Send the wrapped packet to the player.
     *
     * @param player The online Lunar Client user to receive the packet.
     */
    default void send(Player player) {
        send(player, getPacket());
    }

    /**
     * Send any LCPacket to a Lunar Client user. This is used in `send` method above.
     *
     * NOTE: This is intended to send only the wrapped packet, but can be used to
     * send other packets. Although it technically can be done, in most cases it would
     * be better to use {@link LunarClientAPI} to send a packet.
     *
     * @param player An online Lunar client user to receive the packet.
     * @param packet The packet to send to the user.
     */
    default void send(Player player, LCPacket packet) {
        LunarClientAPI.getInstance().sendPacket(player, packet);
    }


}
