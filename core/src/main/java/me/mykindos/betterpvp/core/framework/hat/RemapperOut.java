package me.mykindos.betterpvp.core.framework.hat;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.packet.play.clientbound.WrapperPlayServerEntityEquipment;
import me.mykindos.betterpvp.core.packet.play.clientbound.WrapperPlayServerSetSlot;
import me.mykindos.betterpvp.core.packet.play.clientbound.WrapperPlayServerWindowItems;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class RemapperOut extends PacketAdapter implements Listener {

    private final PacketHatController controller;

    public RemapperOut(Core core, PacketHatController controller) {
        super(core, PacketType.Play.Server.ENTITY_EQUIPMENT,
                PacketType.Play.Server.WINDOW_ITEMS,
                PacketType.Play.Server.SET_SLOT);
        this.controller = controller;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        final PacketType type = event.getPacketType();
        if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            this.onEntityEquipment(event);
        } else if (type == PacketType.Play.Server.WINDOW_ITEMS ) {
            this.onWindowItems(event);
        } else if (type == PacketType.Play.Server.SET_SLOT) {
            this.onSetSlot(event);
        }
    }

    private void onWindowItems(PacketEvent event) {
        final WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event.getPacket());
        if (!UtilInventory.isPlayerInventory(event.getPlayer(), packet.getContainerId())) {
            return; // Not a player inventory
        }

        final List<ItemStack> items = packet.getItems();
        items.set(5, this.controller.getHatItem(event.getPlayer()).orElse(items.get(5)));
        packet.setItems(items);

        packet.setCarriedItem(this.controller.fromHatItem(packet.getCarriedItem()).orElse(null));
    }

    private void onSetSlot(PacketEvent event) {
        final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event.getPacket());
        final Player player = event.getPlayer();
        if (!UtilInventory.isPlayerInventory(player, packet.getContainerId())) {
            return; // Not a player inventory
        }

        if (packet.getSlot() != 5) {
            return; // Return if it's not in the helmet slot
        }

        final Optional<ItemStack> hatItem = this.controller.getHatItem(player);
        if (hatItem.isEmpty()) {
            return; // No hat
        }

        // Replace helmet with hat
        packet.setItemStack(hatItem.get());
    }

    private void onEntityEquipment(PacketEvent event) {
        final WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event.getPacket());
        final Optional<Entity> entityOpt = UtilEntity.getEntity(event.getPlayer().getWorld(), packet.getEntity());
        if (entityOpt.isEmpty()) {
            return; // Entity not found
        }

        final Entity entity = entityOpt.get();
        if (!(entity instanceof Player player)) {
            return; // Only players can wear hats
        }

        final Optional<ItemStack> hatItem = this.controller.getHatItem(player);
        if (hatItem.isEmpty()) {
            return; // No hat
        }

        // Remove any existing helmets
        final List<Pair<EnumWrappers.ItemSlot, ItemStack>> slots = packet.getSlots();
        final Iterator<Pair<EnumWrappers.ItemSlot, ItemStack>> iterator = slots.iterator();
        boolean hasHelmet = false;
        while (iterator.hasNext()) {
            final Pair<EnumWrappers.ItemSlot, ItemStack> pair = iterator.next();
            if (pair.getFirst() == EnumWrappers.ItemSlot.HEAD) {
                hasHelmet = true;
                iterator.remove();
            }
        }

        if (!hasHelmet) {
            return; // No helmet to replace
        }

        // Replace helmet with hat
        slots.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, hatItem.get()));
        packet.setSlots(slots);
    }


}
