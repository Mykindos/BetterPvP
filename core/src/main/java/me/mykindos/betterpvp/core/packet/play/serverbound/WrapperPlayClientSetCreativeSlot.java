package me.mykindos.betterpvp.core.packet.play.serverbound;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import me.mykindos.betterpvp.core.packet.AbstractPacket;
import org.bukkit.inventory.ItemStack;

public class WrapperPlayClientSetCreativeSlot extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.SET_CREATIVE_SLOT;

    public WrapperPlayClientSetCreativeSlot() {
        super(TYPE);
    }

    public WrapperPlayClientSetCreativeSlot(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'slotNum'
     *
     * @return 'slotNum'
     */
    public int getSlotNum() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'slotNum'
     *
     * @param value New value for field 'slotNum'
     */
    public void setSlotNum(int value) {
        this.handle.getIntegers().write(0, value);
    }

    /**
     * Retrieves the value of field 'itemStack'
     *
     * @return 'itemStack'
     */
    public ItemStack getItemStack() {
        return this.handle.getItemModifier().read(0);
    }

    /**
     * Sets the value of field 'itemStack'
     *
     * @param value New value for field 'itemStack'
     */
    public void setItemStack(ItemStack value) {
        this.handle.getItemModifier().write(0, value);
    }

}
