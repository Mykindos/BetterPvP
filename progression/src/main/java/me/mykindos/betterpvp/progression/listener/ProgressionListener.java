package me.mykindos.betterpvp.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.display.TimedComponent;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import me.mykindos.betterpvp.progression.event.PlayerProgressionExperienceEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
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
@Singleton
public class ProgressionListener implements Listener {


    private final ClientManager clientManager;
    private final ProfessionProfileManager professionProfileManager;

    @Inject
    public ProgressionListener(ClientManager clientManager, ProfessionProfileManager professionProfileManager) {
        this.clientManager = clientManager;
        this.professionProfileManager = professionProfileManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGainExperience(PlayerProgressionExperienceEvent event) {
        final Player player = event.getPlayer();
        final Client client = clientManager.search().online(player);
        final Gamer gamer = client.getGamer();
        final double amount = event.getGainedExp();

        // Grant exp
        professionProfileManager.getObject(player.getUniqueId()).ifPresent(profile -> {
            var data = profile.getProfessionDataMap().computeIfAbsent(event.getProfession(), k -> new ProfessionData(player.getUniqueId(), event.getProfession()));
            data.setExperience(data.getExperience() + amount);
            professionProfileManager.getRepository().saveExperience(player.getUniqueId(), event.getProfession(), data.getExperience());
        });

        // Action bar XP
        final String tree = event.getProfession();
        final TextComponent actionBarText;
        if (amount > 0) {
            actionBarText = Component.text("+" + String.format("%.1f", amount) + " " + tree + " XP", NamedTextColor.DARK_AQUA);
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
        gamer.getTitleQueue().add(501, titleCmpt);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0f);
    }

}
