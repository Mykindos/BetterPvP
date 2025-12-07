package me.mykindos.betterpvp.core.framework.hat;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public class RemapperIn implements PacketListener {

    private final PacketHatController controller;
    private final HatProtocol protocol;

    public RemapperIn(PacketHatController controller, HatProtocol protocol) {
        this.controller = controller;
        this.protocol = protocol;
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        final WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
        final Optional<Map<Integer, ItemStack>> slotsOpt = packet.getSlots();
        boolean update = packet.getSlot() == 5;
        if (slotsOpt.isPresent()) {
            final Map<Integer, ItemStack> slots = slotsOpt.get();
            for (Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
                final ItemStack itemStack = entry.getValue();
                final org.bukkit.inventory.ItemStack bukkitStack = SpigotConversionUtil.toBukkitItemStack(itemStack);
                entry.setValue(controller.fromHatItem(bukkitStack)
                        .map(SpigotConversionUtil::fromBukkitItemStack)
                        .orElse(null));
            }
            update = update || slots.containsKey(5);
        }
//        packet.setCarriedItem(controller.fromHatItem(packet.getCarriedItem()).orElse(null));

        // Re-send their hat because they took it off
        if (update) {
            this.protocol.broadcast(event.getPlayer(), true);
        }
    }

}