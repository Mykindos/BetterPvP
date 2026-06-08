package me.mykindos.betterpvp.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.components.professions.PlayerProgressionExperienceEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import me.mykindos.betterpvp.core.utilities.model.display.component.TimedComponent;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
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
        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            if (!profile.isLoaded()) return;
            var data = profile.getProfessionDataMap().computeIfAbsent(event.getProfession(), k -> new ProfessionData(player.getUniqueId(), event.getProfession()));
            data.setExperience(data.getExperience() + amount);
            professionProfileManager.getRepository().saveExperience(player.getUniqueId(), event.getProfession(), data.getExperience());
        });

        // Track XP gained as a client stat
        if (amount > 0) {
            final ClientStat xpStat = switch (event.getProfession()) {
                case "Fishing" -> ClientStat.FISHING_XP;
                case "Woodcutting" -> ClientStat.WOODCUTTING_XP;
                case "Mining" -> ClientStat.MINING_XP;
                default -> null;
            };
            if (xpStat != null) {
                client.getStatContainer().incrementStat(xpStat, amount);
            }
        }

        // Action bar XP
        final String tree = event.getProfession();
        final TextComponent actionBarText;
        if (amount > 0) {
            actionBarText = Component.text("+" + UtilFormat.formatNumber(amount, 1) + " " + tree + " XP", NamedTextColor.DARK_AQUA);
        } else {
            String amountString = UtilFormat.formatNumber(amount);
            if (amount == 0) {
                amountString = "+" + amountString;
            }
            actionBarText = Component.text(amountString + " " + tree + " XP", NamedTextColor.RED);
        }

        final TimedComponent component = new TimedComponent(1.0, false, gmr -> actionBarText);
        gamer.getActionBar().add(250, component);

        // Title levelup
        if (!event.isLevelUp()) {
            if (event.getGainedExp() > 10) {
                //player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            }
            return;
        }

        final int level = event.getLevel();
        final int previous = event.getPreviousLevel();

        if(level >= 99) {
            UtilMessage.broadcast("progression.prefix", "progression.levelup.broadcast",
                    Component.text(player.getName(), NamedTextColor.YELLOW),
                    Component.text(level, NamedTextColor.YELLOW),
                    Component.text(tree));
            for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                UtilSound.playSound(onlinePlayer, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2f, 1f, true);
                UtilSound.playSound(onlinePlayer, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 2f, 1f, true);
                UtilSound.playSound(onlinePlayer, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 2f, 1f, true);
            }
        }

        final Component spendSkillPointsComponent = Translations.component("progression.levelup.spend-points",
                        Component.text("/" + tree.toLowerCase(), NamedTextColor.YELLOW)
                                .decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.runCommand("/" + tree.toLowerCase()))
                                .hoverEvent(HoverEvent.showText(Translations.component("progression.levelup.spend-points-hover").color(NamedTextColor.GRAY))))
                .color(NamedTextColor.GRAY);

        UtilMessage.message(player, tree, "progression.levelup.message",
                Component.text(level, NamedTextColor.GREEN),
                Component.text(tree, NamedTextColor.GREEN));
        UtilMessage.message(player, tree, spendSkillPointsComponent);

            Function<Gamer, Component> title = gmr -> Component.text(tree + " Level Up!", NamedTextColor.GREEN, TextDecoration.BOLD);
            Function<Gamer, Component> subtitle = gmr -> Component.text("Level " + previous + " \u279C " + level, NamedTextColor.DARK_GREEN);
            final TitleComponent titleCmpt = new TitleComponent(0, 2.5, 1, true, title, subtitle);
            gamer.getTitleQueue().add(501, titleCmpt);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

}
