package me.mykindos.betterpvp.core.item.remapper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.HashedStack;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCursorItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.pagination.LorePageService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@BPvPListener
@Singleton
@PluginAdapter("packetevents")
public class ItemPacketRemapper implements PacketListener {

    private final ItemFactory itemFactory;
    private final LorePageService lorePageService;

    @Inject
    private ItemPacketRemapper(Core core, ItemFactory itemFactory, LorePageService lorePageService) {
        this.itemFactory = itemFactory;
        this.lorePageService = lorePageService;
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.LOW);
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
            case PacketType.Play.Server.ENTITY_EQUIPMENT -> this.onEntityEquipment(event);
            case PacketType.Play.Server.COLLECT_ITEM -> this.onCollectItem(event);
            //case PacketType.Play.Server.ENTITY_METADATA -> //this.onEntityMetadata(event);
            default -> { }
        }
    }

    // Map from
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();
        switch (type) {
            case PacketType.Play.Client.CLICK_WINDOW -> this.onWindowClick(event);
            case PacketType.Play.Client.CREATIVE_INVENTORY_ACTION -> this.onCreativeAction(event);
            default -> { }
        }
    }

    private void onCreativeAction(PacketReceiveEvent event) {
        WrapperPlayClientCreativeInventoryAction packet = new WrapperPlayClientCreativeInventoryAction(event);
        final ItemStack stack = packet.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        final org.bukkit.inventory.ItemStack inPlace = player.getOpenInventory().getItem(packet.getSlot());
        final Optional<org.bukkit.inventory.ItemStack> converted = itemFactory.convertItemStack(SpigotConversionUtil.toBukkitItemStack(stack));
        converted.ifPresent(itemStack -> packet.setItemStack(SpigotConversionUtil.fromBukkitItemStack(itemStack)));

        if (inPlace == null) {
            return;
        }

        final Optional<ItemInstance> itemOpt = itemFactory.fromItemStack(inPlace);
        if (itemOpt.isEmpty()) {
            return;
        }

        final ItemInstance item = itemOpt.get();
        final org.bukkit.inventory.ItemStack existing = SpigotConversionUtil.toBukkitItemStack(stack);
        final org.bukkit.inventory.ItemStack view = item.getView().get();
        if (existing.equals(view)) {
            event.setCancelled(true);
        } else {
            packet.setItemStack(mapTo(stack, player));
        }
    }

    private void onCollectItem(PacketSendEvent event) {
//        final WrapperPlayServerCollectItem packet = new WrapperPlayServerCollectItem(event);
//        packet.setItem(mapTo(packet.getItem()));
    }

    private void onWindowClick(PacketReceiveEvent event) {
        final WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
        final Player player = event.getPlayer();

        // Stops duplication
        if (packet.getCarriedItemStack().isEmpty()) {
            final ItemStack carried = getItem(player, packet.getWindowId(), -100);
            if (carried == null) {
                return;
            }

            packet.setCarriedHashedStack(HashedStack.fromItemStack(mapFrom(carried)));
        }

        final Optional<Map<Integer, ItemStack>> slotsOpt = packet.getSlots();
        if (slotsOpt.isEmpty()) {
            return;
        }

        final Map<Integer, ItemStack> slots = slotsOpt.get();
        final Map<Integer, Optional<HashedStack>> hashed = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
            if (entry.getValue() == null) continue;
            final ItemStack slot = mapFrom(entry.getValue());
            hashed.put(entry.getKey(), Optional.of(HashedStack.fromItemStack(slot)));
        }

        packet.setHashedSlots(hashed);
    }

    private void onEntityEquipment(PacketSendEvent event) {
        final WrapperPlayServerEntityEquipment packet;
        try {
            packet = new WrapperPlayServerEntityEquipment(event);
        } catch (Exception e) {
            // Buffer already consumed by another handler in the pipeline
            return;
        }
        final Player viewer = event.getPlayer() instanceof Player player ? player : null;
        for (Equipment equipment : packet.getEquipment()) {
            equipment.setItem(mapTo(equipment.getItem(), viewer));
        }
    }

    private void onSetCursorItem(PacketSendEvent event) {
        final WrapperPlayServerSetCursorItem packet = new WrapperPlayServerSetCursorItem(event);
        final Player viewer = event.getPlayer() instanceof Player player ? player : null;
        packet.setStack(mapTo(packet.getStack(), viewer));
    }

    private void onSetPlayerInventory(PacketSendEvent event) {
        final WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(event);
        final Player viewer = event.getPlayer() instanceof Player player ? player : null;
        packet.setStack(mapTo(packet.getStack(), viewer));
    }

    private void onWindowItems(PacketSendEvent event) {
        final WrapperPlayServerWindowItems packet;
        try {
            packet = new WrapperPlayServerWindowItems(event);
        } catch (Exception e) {
            return;
        }
        final Player viewer = event.getPlayer() instanceof Player player ? player : null;
        final List<ItemStack> items = packet.getItems().stream()
                        .map(item -> mapTo(item, viewer))
                        .toList();
        packet.setItems(new ArrayList<>(items));
        packet.setCarriedItem(mapTo(packet.getCarriedItem().orElse(null), viewer));
    }

    private void onSetSlot(PacketSendEvent event) {
        final WrapperPlayServerSetSlot packet;
        try {
            packet = new WrapperPlayServerSetSlot(event);
        } catch (Exception e) {
            return;
        }

        // We have to do this for creative players because when they receive a SetSlot packet they take it as a true
        // source of truth. This means that the player does not send any feedback packets but the item that is seen
        // is what is converted back to the original item. To avoid this, we send them the true item THEN update it,
        // because the other packets don't cause this
        if (event.getPlayer() instanceof Player player
                && player.getGameMode().equals(GameMode.CREATIVE)
                && packet.getSlot() != 45) { // #updateInventory calls SetSlot packet so this would make it an infinitely recursive call
            player.updateInventory();
            return;
        }

        final Player viewer = event.getPlayer() instanceof Player player ? player : null;
        packet.setItem(mapTo(packet.getItem(), viewer));
    }

    private ItemStack mapTo(ItemStack protocolItemStack, Player viewer) {
        if (protocolItemStack == null) return null;

        try {
            final org.bukkit.inventory.ItemStack previous = SpigotConversionUtil.toBukkitItemStack(protocolItemStack);
            if (previous.isEmpty() || previous.hasItemMeta() && (previous.getItemMeta().hasLore() || previous.getItemMeta().hasDisplayName())) {
                return protocolItemStack;
            }

            final Optional<ItemInstance> itemOpt = itemFactory.fromItemStack(previous.clone());
            final org.bukkit.inventory.ItemStack result = itemOpt.map(itemInstance -> {
                // Render the page this specific viewer is looking at (defaults to most relevant).
                final Integer page = viewer != null ? lorePageService.resolve(viewer.getUniqueId(), itemInstance) : null;
                return itemInstance.getView().get(null, page);
            }).orElse(previous).clone();
            return SpigotConversionUtil.fromBukkitItemStack(result);
        } catch (Exception e) {
            return protocolItemStack;
        }
    }

    private ItemStack mapFrom(ItemStack protocolItemStack) {
        return protocolItemStack;
//    TODO: future version updates will allow us to read hashed items
//
//        final org.bukkit.inventory.ItemStack bukkitItemStack = SpigotConversionUtil.toBukkitItemStack(protocolItemStack);
//        final PersistentDataContainerView pdc = bukkitItemStack.getPersistentDataContainer();
//        System.out.println("In: " + bukkitItemStack.toString());
//        if (!pdc.has(DATA_KEY, PersistentDataType.BYTE_ARRAY)) {
//            return protocolItemStack; // Not a custom item
//        }
//
//
//
//        byte[] bytes = pdc.get(DATA_KEY, PersistentDataType.BYTE_ARRAY);
//        if (bytes == null) {
//            return protocolItemStack; // Not a custom item
//        }
//
//        final org.bukkit.inventory.ItemStack result = org.bukkit.inventory.ItemStack.deserializeBytes(bytes);
//        return SpigotConversionUtil.fromBukkitItemStack(result);
    }

    private AbstractContainerMenu getWindow(Player player, int id) {
        final ServerPlayer netPlayer = ((CraftPlayer) player).getHandle();
        if (id == -2 || netPlayer.inventoryMenu.containerId == id) {
            return netPlayer.inventoryMenu;
        }

        final AbstractContainerMenu containerMenu = netPlayer.containerMenu;
        if (containerMenu == null) {
            return null;
        }

        if (containerMenu.containerId != id) {
            return null;
        }

        return containerMenu;
    }

    private ItemStack getItem(Player player, int windowId, int slot) {
        final AbstractContainerMenu window = getWindow(player, windowId);
        if (window == null) {
            return null;
        }

        net.minecraft.world.item.ItemStack netStack = slot == -100
                ? window.getCarried()
                : window.getSlot(slot).getItem();
        try {
            return SpigotConversionUtil.fromBukkitItemStack(netStack.asBukkitCopy());
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

}
