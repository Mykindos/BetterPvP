package me.mykindos.betterpvp.core.framework.hat;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.packet.play.clientbound.WrapperPlayServerEntityEquipment;
import me.mykindos.betterpvp.core.packet.play.clientbound.WrapperPlayServerSetSlot;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@BPvPListener
@PluginAdapter("ProtocolLib")
public class HatProtocol implements Listener {

    @Inject
    private Core plugin;

    public void broadcast(Player wearer, boolean others) {
        // We broadcast player's helmet because RemapperOut maps it to the hat item after
        final ItemStack helmet = wearer.getInventory().getHelmet();

        if (others) {
            // Others, including self
            final WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment();
            packet.setEntity(wearer.getEntityId());
            packet.setSlots(List.of(new Pair<>(EnumWrappers.ItemSlot.HEAD, helmet)));
            packet.broadcastPacket();
        } else {
            // Just send to self
            final WrapperPlayServerSetSlot packet2 = new WrapperPlayServerSetSlot();
            packet2.setContainerId(0); // Allows changing player inventory
            packet2.setSlot(5);
            packet2.setItemStack(helmet);
            packet2.sendPacket(wearer);
        }

    }

    @EventHandler
    public void onArmor(PlayerArmorChangeEvent event) {
        if (event.getSlotType() != PlayerArmorChangeEvent.SlotType.HEAD) {
            return; // Skip non-head
        }

        UtilServer.runTaskLater(plugin, () -> broadcast(event.getPlayer(), true), 5L);
    }

}
