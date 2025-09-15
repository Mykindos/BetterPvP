package me.mykindos.betterpvp.core.framework.hat;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.google.inject.Inject;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Collections;

@BPvPListener
public class HatProtocol implements Listener {

    @Inject
    private Core plugin;

    public void broadcast(Player wearer, boolean others) {
        // We broadcast player's helmet because RemapperOut maps it to the hat item after
        final org.bukkit.inventory.ItemStack vanillaItem = wearer.getInventory().getHelmet();
        final ItemStack helmet = SpigotConversionUtil.fromBukkitItemStack(vanillaItem);

        if (others) {
            // Others, including self
            final WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(
                    wearer.getEntityId(),
                    Collections.singletonList(new Equipment(EquipmentSlot.HELMET, helmet))
            );

            for (Player player : wearer.getTrackedBy()) {
                PacketEvents.getAPI().getPlayerManager().getUser(player).sendPacket(packet);
            }
        }

        // Just send to self
        final WrapperPlayServerSetSlot packet2 = new WrapperPlayServerSetSlot(0,
                0, // Allows changing player inventory
                5,
                helmet);
        PacketEvents.getAPI().getPlayerManager().getUser(wearer).sendPacket(packet2);
    }

    @EventHandler
    public void onArmor(PlayerArmorChangeEvent event) {
        if (event.getSlotType() != PlayerArmorChangeEvent.SlotType.HEAD) {
            return; // Skip non-head
        }

        UtilServer.runTaskLater(plugin, () -> broadcast(event.getPlayer(), true), 5L);
    }

}