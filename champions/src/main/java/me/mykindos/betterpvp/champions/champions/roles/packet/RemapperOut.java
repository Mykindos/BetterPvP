package me.mykindos.betterpvp.champions.champions.roles.packet;

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
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RemapperOut implements PacketListener {

    static final NamespacedKey FLAG = new NamespacedKey("betterpvp", "role_placeholder");
    private final RoleManager roleManager;

    public RemapperOut(RoleManager roleManager) {
        this.roleManager = roleManager;
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

        Role role = this.roleManager.getRole(event.getPlayer());

        // Update contents
        final List<ItemStack> items = packet.getItems();
        for (int i = 5; i <= 8; i++) { // Armor slots
            final ItemStack current = items.get(i);
            if (!SpigotConversionUtil.toBukkitItemStack(current).getType().isAir()) {
                continue; // We don't want to override existing armor pieces
            }

            items.set(i, SpigotConversionUtil.fromBukkitItemStack(this.getRolePlaceholder(getSlot(i), role)));
        }
        packet.setItems(items);
    }

    private void onSetSlot(PacketSendEvent event) {
        final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
        final Player player = event.getPlayer();
        if (!UtilInventory.isPlayerInventory(player, packet.getWindowId())) {
            return; // Not a player inventory
        }

        if (packet.getSlot() < 5 || packet.getSlot() > 8) {
            return; // Return if it's not an armor slot
        }

        if (!SpigotConversionUtil.toBukkitItemStack(packet.getItem()).getType().isAir())  {
            return; // We don't want to override existing armor pieces
        }

        // Replace air with role placeholder
        final Role role = this.roleManager.getRole(player);
        final org.bukkit.inventory.ItemStack placeholder = this.getRolePlaceholder(getSlot(packet.getSlot()), role);
        packet.setItem(SpigotConversionUtil.fromBukkitItemStack(placeholder));
    }

    private void onEntityEquipment(PacketSendEvent event) {
        final WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event);
        final Player receiver = event.getPlayer();
        final Entity entity = SpigotConversionUtil.getEntityById(receiver.getWorld(), packet.getEntityId());
        if (!(entity instanceof LivingEntity livingEntity)) {
            return; // Only living entities
        }

        final @NotNull Optional<Role> roleOpt = this.roleManager.getRole(livingEntity);
        if (roleOpt.isEmpty()) {
            return; // No role, no update
        }

        // Disable replacement of existing armor pieces so we don't override them
        final List<Equipment> slots = new ArrayList<>(packet.getEquipment());
        final List<EquipmentSlot> toReplace = new ArrayList<>(List.of(EquipmentSlot.HELMET,
                EquipmentSlot.CHEST_PLATE,
                EquipmentSlot.LEGGINGS,
                EquipmentSlot.BOOTS));
        for (Equipment slot : slots) {
            toReplace.remove(slot.getSlot());
        }

        // Replace pieces
        final Role role = roleOpt.get();
        for (EquipmentSlot equipmentSlot : toReplace) {
            final org.bukkit.inventory.ItemStack rolePlaceholder = this.getRolePlaceholder(equipmentSlot, role);
            slots.add(new Equipment(equipmentSlot, SpigotConversionUtil.fromBukkitItemStack(rolePlaceholder)));
        }

        // Update packet
        packet.setEquipment(slots);
    }

    private EquipmentSlot getSlot(int slot) {
        return switch (slot) {
            case 5 -> EquipmentSlot.HELMET;
            case 6 -> EquipmentSlot.CHEST_PLATE;
            case 7 -> EquipmentSlot.LEGGINGS;
            case 8 -> EquipmentSlot.BOOTS;
            default -> throw new IllegalStateException("Unexpected value: " + slot);
        };
    }

    private org.bukkit.inventory.ItemStack getRolePlaceholder(EquipmentSlot slot, Role role) {
        Key itemModel = Key.key("betterpvp", "menu/sprite/" + role.name().toLowerCase() + "_indicator");
        final org.bukkit.inventory.ItemStack item = ItemView.builder()
                .material(role.getMaterial(switch (slot) {
                    case BOOTS -> org.bukkit.inventory.EquipmentSlot.FEET;
                    case LEGGINGS -> org.bukkit.inventory.EquipmentSlot.LEGS;
                    case CHEST_PLATE -> org.bukkit.inventory.EquipmentSlot.CHEST;
                    case HELMET -> org.bukkit.inventory.EquipmentSlot.HEAD;
                    default -> throw new IllegalStateException("Unexpected value: " + slot);
                }))
                .itemModel(itemModel)
                .hideTooltip(true)
                .build()
                .get();
        item.editPersistentDataContainer(container -> {
            container.set(FLAG, PersistentDataType.BOOLEAN, true);
        });
        return item;
    }
}