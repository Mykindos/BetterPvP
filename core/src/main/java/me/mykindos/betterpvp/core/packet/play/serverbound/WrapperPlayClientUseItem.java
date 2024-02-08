package me.mykindos.betterpvp.core.packet.play.serverbound;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.Hand;
import me.mykindos.betterpvp.core.packet.AbstractPacket;

public class WrapperPlayClientUseItem extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.USE_ITEM;

    public WrapperPlayClientUseItem() {
        super(TYPE);
    }

    public WrapperPlayClientUseItem(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'hand'
     *
     * @return 'hand'
     */
    public Hand getHand() {
        return this.handle.getHands().read(0);
    }

    /**
     * Sets the value of field 'hand'
     *
     * @param value New value for field 'hand'
     */
    public void setHand(Hand value) {
        this.handle.getHands().write(0, value);
    }

    /**
     * Retrieves the value of field 'sequence'
     *
     * @return 'sequence'
     */
    public int getSequence() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'sequence'
     *
     * @param value New value for field 'sequence'
     */
    public void setSequence(int value) {
        this.handle.getIntegers().write(0, value);
    }

    /**
     * Retrieves the value of field 'timestamp'
     *
     * @return 'timestamp'
     */
    public long getTimestamp() {
        return this.handle.getLongs().read(0);
    }

    /**
     * Sets the value of field 'timestamp'
     *
     * @param value New value for field 'timestamp'
     */
    public void setTimestamp(long value) {
        this.handle.getLongs().write(0, value);
    }

}
