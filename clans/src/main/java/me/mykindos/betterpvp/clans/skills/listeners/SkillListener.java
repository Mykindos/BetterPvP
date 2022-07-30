package me.mykindos.betterpvp.clans.skills.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.skills.Skill;
import me.mykindos.betterpvp.clans.skills.SkillManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

@BPvPListener
public class SkillListener implements Listener {

    @Inject
    private SkillManager manager;

    @UpdateEvent(delay = 5000)
    public void test() {
        Optional<Skill> leapOption = manager.getObject("Leap");
        leapOption.ifPresent(skill -> System.out.println(skill.getSkillConfig().getCooldown()));
    }

    @EventHandler
    public void onSkillActivate(PlayerInteractEvent event) {

        if(event.getHand() == EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();

        /*
         * Scan builds, check items, check action, activate skill
         */
    }
}
