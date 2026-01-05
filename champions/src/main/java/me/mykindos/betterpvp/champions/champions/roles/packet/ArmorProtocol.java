package me.mykindos.betterpvp.champions.champions.roles.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;

import java.util.List;
import java.util.Objects;

@BPvPListener
@Singleton
public class ArmorProtocol implements Listener {

    @Inject
    private Core plugin;

    public void broadcast(LivingEntity wearer, boolean others) {
        if (wearer.getEquipment() == null) {
            return;
        }

        // We broadcast player's armor because RemapperOut maps it to the armor contents after
        final ItemStack helmet = getArmor(wearer, org.bukkit.inventory.EquipmentSlot.HEAD);
        final ItemStack chestplate = getArmor(wearer, org.bukkit.inventory.EquipmentSlot.CHEST);
        final ItemStack leggings = getArmor(wearer, org.bukkit.inventory.EquipmentSlot.LEGS);
        final ItemStack boots = getArmor(wearer, org.bukkit.inventory.EquipmentSlot.FEET);

        if (others) {
            // Others, including self
            final WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(
                    wearer.getEntityId(),
                    List.of(new Equipment(EquipmentSlot.HELMET, helmet),
                            new Equipment(EquipmentSlot.CHEST_PLATE, chestplate),
                            new Equipment(EquipmentSlot.LEGGINGS, leggings),
                            new Equipment(EquipmentSlot.BOOTS, boots))
            );

            for (Player player : wearer.getTrackedBy()) {
                PacketEvents.getAPI().getPlayerManager().getUser(player).sendPacket(packet);
            }
        }

        // Just send to self if we're a player
        if (wearer instanceof Player player) {
            player.updateInventory();
            sendSetSlot(player, 5, helmet);
            sendSetSlot(player, 6, chestplate);
            sendSetSlot(player, 7, leggings);
            sendSetSlot(player, 8, boots);
        }
    }

    private void sendSetSlot(Player wearer, int slot, ItemStack itemStack) {
        final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(0,
                0, // Allows changing player inventory
                slot,
                itemStack);
        PacketEvents.getAPI().getPlayerManager().getUser(wearer).sendPacket(packet);
    }

    private ItemStack getArmor(LivingEntity wearer, org.bukkit.inventory.EquipmentSlot equipmentSlot) {
        final EntityEquipment equipment = Objects.requireNonNull(wearer.getEquipment());
        final org.bukkit.inventory.ItemStack vanillaItem = equipment.getItem(equipmentSlot);
        return SpigotConversionUtil.fromBukkitItemStack(vanillaItem);
    }

    @EventHandler
    public void onArmor(EntityEquipmentChangedEvent event) {
        UtilServer.runTaskLater(plugin, () -> broadcast(event.getEntity(), true), 4L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onRoleChange(RoleChangeEvent event) {
        broadcast(event.getLivingEntity(), true);
    }

}