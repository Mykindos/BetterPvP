package me.mykindos.betterpvp.core.item.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.window.AbstractWindow;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.inventory.window.WindowManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.packet.play.clientbound.WrapperPlayServerSetSlot;
import me.mykindos.betterpvp.core.packet.play.clientbound.WrapperPlayServerWindowItems;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

@BPvPListener
@Singleton
public class ItemRemapperOut extends PacketAdapter implements Listener {

    private final ItemFactory itemFactory;

    @Inject
    private ItemRemapperOut(Core core, ItemFactory itemFactory) {
        super(core, ListenerPriority.LOWEST, PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT);
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
        this.itemFactory = itemFactory;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        final PacketType type = event.getPacketType();
        if (type == PacketType.Play.Server.WINDOW_ITEMS) {
            this.onWindowItems(event);
        } else if (type == PacketType.Play.Server.SET_SLOT) {
            this.onSetSlot(event);
        }
    }

    private void onWindowItems(PacketEvent event) {
        final WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event.getPacket());
        final List<ItemStack> items = packet.getItems().stream()
                        .map(this::map)
                        .toList();
        packet.setItems(items);
        packet.setCarriedItem(packet.getCarriedItem());
    }

    private void onSetSlot(PacketEvent event) {
        final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event.getPacket());
        packet.setItemStack(map(packet.getItemStack()));
    }

    private ItemStack map(ItemStack itemStack) {
        if (itemStack == null) {
            return null; // Return null if the item stack is null
        }

        final Optional<ItemInstance> itemOpt = itemFactory.fromItemStack(itemStack);
        return itemOpt.map(itemInstance -> itemInstance.getView().get()).orElse(itemStack);
    }

}
