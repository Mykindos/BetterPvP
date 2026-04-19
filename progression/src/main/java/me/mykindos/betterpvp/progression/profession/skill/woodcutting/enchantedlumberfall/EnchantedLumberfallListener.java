package me.mykindos.betterpvp.progression.profession.skill.woodcutting.enchantedlumberfall;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.impl.interaction.event.TreeFellerEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class EnchantedLumberfallListener implements Listener {

    @Inject
    private EnchantedLumberfall skill;

    @EventHandler
    public void whenPlayerFellsTree(TreeFellerEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.whenPlayerFellsTree(event);
    }
}
