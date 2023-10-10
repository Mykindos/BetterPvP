package me.mykindos.betterpvp.clans.world;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

@BPvPListener
public class DoorListener implements Listener {

    private final ClanManager clanManager;
    private final CooldownManager cooldownManager;

    @Inject
    public DoorListener(ClanManager clanManager, CooldownManager cooldownManager) {
        this.clanManager = clanManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (block.getType() == Material.IRON_DOOR || block.getType() == Material.IRON_TRAPDOOR) {

            if (!clanManager.hasAccess(event.getPlayer(), event.getPlayer().getLocation())) {
                if (cooldownManager.use(event.getPlayer(), "Door", 250, false)) {
                    block.getWorld().playEffect(block.getLocation(), Effect.ZOMBIE_CHEW_WOODEN_DOOR, 0);
                }

                event.setCancelled(true);
                return;
            }


            BlockData doorData = block.getBlockData();
            Openable door = (Openable) doorData;

            if (door.isOpen()) {
                door.setOpen(false);
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f);
            } else {
                door.setOpen(true);
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1f, 1f);
            }

            block.setBlockData(doorData);
            block.getState().update();

            var ep = ((CraftPlayer) event.getPlayer()).getHandle();
            ClientboundAnimatePacket packet = new ClientboundAnimatePacket(ep, 0);
            ep.connection.send(packet);
            event.setCancelled(true);
        }

    }
}
