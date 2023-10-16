package me.mykindos.betterpvp.progression.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.display.TimedComponent;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.ProgressionsManager;
import me.mykindos.betterpvp.progression.event.PlayerProgressionExperienceEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.function.Function;

@BPvPListener
public class ProgressionListener implements Listener {

    @Inject
    private GamerManager gamerManager;

    @Inject
    private ProgressionsManager progressionsManager;

    @Inject
    private Progression progression;

//    @UpdateEvent(delay = 5_000, isAsync = true)
    @UpdateEvent(delay = 60 * 5 * 1000, isAsync = true)
    public void cycleSave() {
        progressionsManager.getTrees().forEach(tree -> tree.getStatsRepository().saveAll(true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGainExperience(PlayerProgressionExperienceEvent event) {
        final Player player = event.getPlayer();
        gamerManager.getObject(player.getUniqueId()).ifPresent(gamer -> {
            // Action bar XP
            final String tree = event.getTree().getName();
            final long amount = event.getGainedExp();
            final TextComponent actionBarText;
            if (amount > 0) {
                actionBarText = Component.text("+" + amount + " " + tree + " XP", NamedTextColor.DARK_AQUA);
            } else {
                String amountString = String.valueOf(amount);
                if (amount == 0) {
                    amountString = "+" + amountString;
                }
                actionBarText = Component.text(amountString + " " + tree + " XP", NamedTextColor.RED);
            }

            final TimedComponent component = new TimedComponent(1.0, false, gmr -> actionBarText);
            gamer.getActionBar().add(250, component);

            // Title levelup
            if (!event.isLevelUp()) {
                return;
            }

            final int level = event.getLevel();
            final int previous = event.getPreviousLevel();

            Function<Gamer, Component> title = gmr -> Component.text(tree + " Level Up!", NamedTextColor.GREEN, TextDecoration.BOLD);
            Function<Gamer, Component> subtitle = gmr -> Component.text("Level " + previous + " \u279C " + level, NamedTextColor.DARK_GREEN);
            final TitleComponent titleCmpt = new TitleComponent(0, 2.5, 1, true, title, subtitle);
            gamer.getTitleQueue().add(250, titleCmpt);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0f);
        });
    }

}
