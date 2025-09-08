package me.mykindos.betterpvp.core.framework.hat;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class RemapperOut implements PacketListener {

    private final PacketHatController controller;

    public RemapperOut(PacketHatController controller) {
        this.controller = controller;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        final PacketTypeCommon type = event.getPacketType();
        switch (type) {
            case PacketType.Play.Server.ENTITY_EQUIPMENT -> this.onEntityEquipment(event);
            case PacketType.Play.Server.WINDOW_ITEMS -> this.onWindowItems(event);
            case PacketType.Play.Server.SET_SLOT -> this.onSetSlot(event);
            default -> { }
        }
    }

    private void onWindowItems(PacketSendEvent event) {
        final WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
        if (!UtilInventory.isPlayerInventory(event.getPlayer(), packet.getWindowId())) {
            return; // Not a player inventory
        }

        final List<ItemStack> items = packet.getItems();
        items.set(5, this.controller.getHatItem(event.getPlayer())
                .map(SpigotConversionUtil::fromBukkitItemStack)
                .orElse(items.get(5)));
        packet.setItems(items);

        packet.setCarriedItem(packet.getCarriedItem()
                .flatMap(stack -> this.controller.fromHatItem(SpigotConversionUtil.toBukkitItemStack(stack)))
                .map(SpigotConversionUtil::fromBukkitItemStack)
                .orElse(null));
    }

    private void onSetSlot(PacketSendEvent event) {
        final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
        final Player player = event.getPlayer();
        if (!UtilInventory.isPlayerInventory(player, packet.getWindowId())) {
            return; // Not a player inventory
        }

        if (packet.getSlot() != 5) {
            return; // Return if it's not in the helmet slot
        }

        final Optional<org.bukkit.inventory.ItemStack> hatItem = this.controller.getHatItem(player);
        if (hatItem.isEmpty()) {
            return; // No hat
        }

        // Replace helmet with hat
        packet.setItem(SpigotConversionUtil.fromBukkitItemStack(hatItem.get()));
    }

    private void onEntityEquipment(PacketSendEvent event) {
        final WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event);
        final Player receiver = event.getPlayer();
        final Entity entity = SpigotConversionUtil.getEntityById(receiver.getWorld(), packet.getEntityId());
        if (!(entity instanceof Player player)) {
            return; // Only players can wear hats
        }

        final Optional<org.bukkit.inventory.ItemStack> hatItem = this.controller.getHatItem(player);
        if (hatItem.isEmpty()) {
            return; // No hat
        }

        // Remove any existing helmets
        final List<Equipment> slots = new ArrayList<>(packet.getEquipment());
        final Iterator<Equipment> iterator = slots.iterator();
        boolean hasHelmet = false;
        while (iterator.hasNext()) {
            final Equipment equipment = iterator.next();
            if (equipment.getSlot() == EquipmentSlot.HELMET) {
                hasHelmet = true;
                iterator.remove();
            }
        }

        if (!hasHelmet) {
            return; // No helmet to replace
        }

        // Replace helmet with hat
        slots.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(hatItem.get())));
        packet.setEquipment(slots);
    }
}