package me.mykindos.betterpvp.core.framework.hat;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.packet.play.serverbound.WrapperPlayClientWindowClick;
import org.bukkit.inventory.ItemStack;

public class RemapperIn extends PacketAdapter {

    private final PacketHatController controller;
    private final HatProtocol protocol;

    public RemapperIn(Core plugin, PacketHatController controller, HatProtocol protocol) {
        super(plugin, PacketType.Play.Client.WINDOW_CLICK);
        this.controller = controller;
        this.protocol = protocol;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        final WrapperPlayClientWindowClick packet = new WrapperPlayClientWindowClick(event.getPacket());
        final Int2ObjectMap<ItemStack> slots = packet.getChangedSlots();
        for (Int2ObjectMap.Entry<ItemStack> entry : slots.int2ObjectEntrySet()) {
            entry.setValue(controller.fromHatItem(entry.getValue()).orElse(null));
        }
        packet.setCarriedItem(controller.fromHatItem(packet.getCarriedItem()).orElse(null));

        // Re-send their hat because they took it off
        if (slots.containsKey(5)) {
            this.protocol.broadcast(event.getPlayer(), false);
        }
    }


}
