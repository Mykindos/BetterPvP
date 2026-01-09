package me.mykindos.betterpvp.champions.champions.roles.packet;

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

    private final ArmorProtocol protocol;

    public RemapperIn(ArmorProtocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        final WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
        final Optional<Map<Integer, ItemStack>> slotsOpt = packet.getSlots();
        boolean update = packet.getSlot() >= 5 && packet.getSlot() <= 8; // Raw click

        // Secondary updated slots
        if (slotsOpt.isPresent()) {
            final Map<Integer, ItemStack> slots = slotsOpt.get();
            for (Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
                final ItemStack itemStack = entry.getValue();
                final org.bukkit.inventory.ItemStack bukkitStack = SpigotConversionUtil.toBukkitItemStack(itemStack);
                if (isPlaceholder(bukkitStack)) {
                    entry.setValue(null); // Replace with air, this item does NOT exist server-side
                }
            }
            update = update || slots.keySet().stream().anyMatch(slot -> slot >= 5 && slot <= 8);
        }
//        packet.setCarriedItem(controller.fromHatItem(packet.getCarriedItem()).orElse(null));

        // Re-send their armor because they took it off
        if (update) {
            this.protocol.broadcast(event.getPlayer(), true);
        }
    }

    private boolean isPlaceholder(org.bukkit.inventory.ItemStack itemStack) {
        return itemStack.getPersistentDataContainer().has(RemapperOut.FLAG);
    }

}