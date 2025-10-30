package me.mykindos.betterpvp.core.menu.quick;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCursorItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.List;

@PluginAdapter("packetevents")
@Singleton
public class QuickMenuPacketListener implements PacketListener {

    private final ClientManager clientManager;

    @Inject
    private QuickMenuPacketListener(ClientManager clientManager) {
        this.clientManager = clientManager;
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.LOWEST);
    }

    // Map from
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();
        switch (type) {
            case PacketType.Play.Client.CLICK_WINDOW -> this.onWindowClick(event);
            default -> { }
        }
    }

    // Map to
    @Override
    public void onPacketSend(PacketSendEvent event) {
        final PacketTypeCommon type = event.getPacketType();
        switch (type) {
            case PacketType.Play.Server.WINDOW_ITEMS -> this.onWindowItems(event);
            case PacketType.Play.Server.SET_SLOT -> this.onSetSlot(event);
            case PacketType.Play.Server.SET_PLAYER_INVENTORY -> this.onSetPlayerInventory(event);
            case PacketType.Play.Server.SET_CURSOR_ITEM -> this.onSetCursorItem(event);
            default -> { }
        }
    }

    private void onWindowClick(PacketReceiveEvent event) {
        final WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
        if (!UtilInventory.isPlayerInventory(event.getPlayer(), packet.getWindowId())) {
            return;
        }

        final Player player = event.getPlayer();
        final Client client = clientManager.search().online(player);

        if (QuickMenu.useQuickMenuButton(client, player, packet.getSlot())) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    private void onSetCursorItem(PacketSendEvent event) {
        final WrapperPlayServerSetCursorItem packet = new WrapperPlayServerSetCursorItem(event);
        packet.setStack(mapFrom(packet.getStack()));
    }

    private void onSetPlayerInventory(PacketSendEvent event) {
        final WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(event);
        final int slot = packet.getSlot();
        // packet.setStack(mapTo(packet.getStack(), slot));
    }

    private void onWindowItems(PacketSendEvent event) {
        final WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);

        if (!UtilInventory.isPlayerInventory(event.getPlayer(), packet.getWindowId())) {
            return; // Not player inventory
        }

        final List<ItemStack> items = packet.getItems();
        for (int i = 1; i <= 4; i++) {
            final ItemProvider quickMenuButton = QuickMenu.getQuickMenuButton(i);
            items.set(i, SpigotConversionUtil.fromBukkitItemStack(quickMenuButton.get()));
        }
        packet.setItems(items);
    }

    private void onSetSlot(PacketSendEvent event) {
        final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
        if (!UtilInventory.isPlayerInventory(event.getPlayer(), packet.getWindowId())) {
            return;
        }

        final ItemStack result = mapTo(packet.getItem(), packet.getSlot());
        if (result != null) {
            packet.setItem(result);
        }
    }

    private ItemStack mapTo(ItemStack protocolItemStack, int slot) {
        final ItemProvider quickMenuButton = QuickMenu.getQuickMenuButton(slot);
        if (quickMenuButton == null) return null;
        return SpigotConversionUtil.fromBukkitItemStack(quickMenuButton.get());
    }

    private ItemStack mapFrom(ItemStack protocolItemStack) {
        if (protocolItemStack == null) return null;

        final org.bukkit.inventory.ItemStack previous = SpigotConversionUtil.toBukkitItemStack(protocolItemStack);
        if (previous.isEmpty() || !QuickMenu.isQuickMenuButton(previous)) {
            return protocolItemStack;
        }

        return ItemStack.EMPTY; // Return air because we don't want them to get those items
    }

    private AbstractContainerMenu getWindow(Player player, int id) {
        final ServerPlayer netPlayer = ((CraftPlayer) player).getHandle();
        if (id == -2 || netPlayer.inventoryMenu.containerId == id) {
            return netPlayer.inventoryMenu;
        }

        Preconditions.checkState(netPlayer.containerMenu != null, "Player has no open container");
        Preconditions.checkState(netPlayer.containerMenu.containerId == id, "Container ID does not match");
        return netPlayer.containerMenu;
    }

    private ItemStack getItem(Player player, int windowId, int slot) {
        final AbstractContainerMenu window = getWindow(player, windowId);
        net.minecraft.world.item.ItemStack netStack = slot == -100
                ? window.getCarried()
                : window.getSlot(slot).getItem();
        return SpigotConversionUtil.fromBukkitItemStack(netStack.asBukkitCopy());
    }
}
