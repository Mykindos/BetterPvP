package me.mykindos.betterpvp.core.item.model.override;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCursorItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Swaps the {@link DataComponentTypes#ITEM_MODEL} of an item as it leaves the server, per holder, without ever
 * touching the stored ItemStack.
 * <p>
 * An override is a {@code from -> to} model pair scoped to one holder: every outgoing item belonging to that
 * holder whose model is {@code from} is rendered as {@code to} instead. Matching on the model key rather than
 * on an item identity means the swap follows the item through the hotbar, the inventory and other players'
 * equipment views without any slot bookkeeping.
 * <p>
 * Because the override lives only in memory, transient visual state cannot outlive the server that set it —
 * a crash mid-state leaves the real item untouched.
 */
@Singleton
@BPvPListener
@PluginAdapter("packetevents")
public class ItemModelOverrideService implements PacketListener, Listener {

    private final Map<UUID, Map<Key, Key>> overrides = new ConcurrentHashMap<>();

    @Inject
    private ItemModelOverrideService() {
        // Runs after ItemPacketRemapper (LOW) so we rewrite the already-expanded view rather than the bare item
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.NORMAL);
    }

    /**
     * Renders every item held by this player whose model is {@code from} as {@code to}, and resends what they
     * and their viewers are currently looking at.
     */
    public void set(Player holder, Key from, Key to) {
        overrides.computeIfAbsent(holder.getUniqueId(), id -> new ConcurrentHashMap<>()).put(from, to);
        refresh(holder);
    }

    /**
     * Drops the override registered for {@code from}, restoring the item's real model.
     */
    public void clear(Player holder, Key from) {
        final Map<Key, Key> held = overrides.get(holder.getUniqueId());
        if (held == null || held.remove(from) == null) {
            return; // Nothing was overridden
        }

        if (held.isEmpty()) {
            overrides.remove(holder.getUniqueId());
        }
        refresh(holder);
    }

    public void clearAll(Player holder) {
        if (overrides.remove(holder.getUniqueId()) != null) {
            refresh(holder);
        }
    }

    /**
     * Resends the holder's inventory to themselves and their held item to everyone tracking them, so a state
     * change is seen immediately instead of on the next natural inventory update.
     */
    public void refresh(Player holder) {
        holder.updateInventory();

        final ItemStack mainHand;
        try {
            mainHand = SpigotConversionUtil.fromBukkitItemStack(holder.getInventory().getItemInMainHand());
        } catch (Exception e) {
            return;
        }

        final WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(holder.getEntityId(),
                Collections.singletonList(new Equipment(EquipmentSlot.MAIN_HAND, mainHand)));
        for (Player viewer : holder.getTrackedBy()) {
            PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(packet);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        overrides.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        overrides.remove(event.getEntity().getUniqueId());
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        final PacketTypeCommon type = event.getPacketType();
        switch (type) {
            case PacketType.Play.Server.ENTITY_EQUIPMENT -> this.onEntityEquipment(event);
            case PacketType.Play.Server.SET_SLOT -> this.onSetSlot(event);
            case PacketType.Play.Server.WINDOW_ITEMS -> this.onWindowItems(event);
            case PacketType.Play.Server.SET_PLAYER_INVENTORY -> this.onSetPlayerInventory(event);
            case PacketType.Play.Server.SET_CURSOR_ITEM -> this.onSetCursorItem(event);
            default -> { }
        }
    }

    private void onEntityEquipment(PacketSendEvent event) {
        if (overrides.isEmpty()) {
            return;
        }

        final WrapperPlayServerEntityEquipment packet;
        try {
            packet = new WrapperPlayServerEntityEquipment(event);
        } catch (Exception e) {
            return; // Buffer already consumed by another handler in the pipeline
        }

        if (!(event.getPlayer() instanceof Player receiver)) {
            return;
        }

        // The holder is the entity wearing the equipment, not the player being sent the packet
        final Entity entity = SpigotConversionUtil.getEntityById(receiver.getWorld(), packet.getEntityId());
        if (!(entity instanceof Player holder) || !overrides.containsKey(holder.getUniqueId())) {
            return;
        }

        final List<Equipment> equipment = new ArrayList<>(packet.getEquipment());
        for (Equipment slot : equipment) {
            slot.setItem(apply(slot.getItem(), holder.getUniqueId()));
        }
        packet.setEquipment(equipment);
    }

    private void onSetSlot(PacketSendEvent event) {
        final UUID holder = viewerId(event);
        if (holder == null) return;

        final WrapperPlayServerSetSlot packet;
        try {
            packet = new WrapperPlayServerSetSlot(event);
        } catch (Exception e) {
            return;
        }
        packet.setItem(apply(packet.getItem(), holder));
    }

    private void onWindowItems(PacketSendEvent event) {
        final UUID holder = viewerId(event);
        if (holder == null) return;

        final WrapperPlayServerWindowItems packet;
        try {
            packet = new WrapperPlayServerWindowItems(event);
        } catch (Exception e) {
            return;
        }

        final List<ItemStack> items = packet.getItems().stream()
                .map(item -> apply(item, holder))
                .toList();
        packet.setItems(new ArrayList<>(items));
    }

    private void onSetPlayerInventory(PacketSendEvent event) {
        final UUID holder = viewerId(event);
        if (holder == null) return;

        final WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(event);
        packet.setStack(apply(packet.getStack(), holder));
    }

    private void onSetCursorItem(PacketSendEvent event) {
        final UUID holder = viewerId(event);
        if (holder == null) return;

        final WrapperPlayServerSetCursorItem packet = new WrapperPlayServerSetCursorItem(event);
        packet.setStack(apply(packet.getStack(), holder));
    }

    /**
     * For a player's own inventory views the recipient is also the holder, so these packets only need
     * remapping when the recipient themselves has an override registered.
     */
    private UUID viewerId(PacketSendEvent event) {
        if (overrides.isEmpty() || !(event.getPlayer() instanceof Player viewer)) {
            return null;
        }
        return overrides.containsKey(viewer.getUniqueId()) ? viewer.getUniqueId() : null;
    }

    private ItemStack apply(ItemStack protocolItemStack, UUID holder) {
        if (protocolItemStack == null) {
            return null;
        }

        final Map<Key, Key> held = overrides.get(holder);
        if (held == null || held.isEmpty()) {
            return protocolItemStack;
        }

        try {
            final org.bukkit.inventory.ItemStack bukkit = SpigotConversionUtil.toBukkitItemStack(protocolItemStack);
            if (bukkit.isEmpty() || !bukkit.hasData(DataComponentTypes.ITEM_MODEL)) {
                return protocolItemStack;
            }

            final Key target = held.get(bukkit.getData(DataComponentTypes.ITEM_MODEL));
            if (target == null) {
                return protocolItemStack;
            }

            final org.bukkit.inventory.ItemStack copy = bukkit.clone();
            copy.setData(DataComponentTypes.ITEM_MODEL, target);
            return SpigotConversionUtil.fromBukkitItemStack(copy);
        } catch (Exception e) {
            return protocolItemStack;
        }
    }
}
