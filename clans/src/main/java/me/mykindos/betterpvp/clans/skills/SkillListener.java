package me.mykindos.betterpvp.clans.skills;

import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

@BPvPListener
public class SkillListener implements Listener {

    @EventHandler
    public void onSkillActivate(PlayerInteractEvent event) {

        if(event.getHand() == EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();

        /*
         * Scan builds, check items, check action, activate skill
         */
    }
}
