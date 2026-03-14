package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.TutorialSubCommand;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClanWrapperStat;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@BPvPListener
public class ClanLoginListener implements Listener {
    private final RealmManager realmManager;

    @Inject
    public ClanLoginListener(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

    @EventHandler()
    public void onLogin(ClientJoinEvent event) {
        final ClanWrapperStat timePlayedStat = ClanWrapperStat.builder()
                .wrappedStat(ClientStat.TIME_PLAYED)
                .build();
        final Season season = Core.getCurrentRealm().getSeason();
        long timePlayed = timePlayedStat.getStat(event.getClient().getStatContainer(), StatFilterType.SEASON, season);
        if (timePlayed <= 1000 * 60 * 60) {
            UtilMessage.message(event.getPlayer(), "Clans", UtilMessage.deserialize("Welcome to <gold>Clans</gold>! It looks like this is your first time playing this season. ")
                    .append(Component.text("Click to join our discord!", NamedTextColor.DARK_PURPLE)
                            .decoration(TextDecoration.UNDERLINED, true)
                            .hoverEvent(HoverEvent.showText(Component.text("Click to join our discord!")))
                            .clickEvent(ClickEvent.openUrl("https://discord.gg/PE32pYfZn9"))
                    )
                    .appendNewline()
                    .append(UtilMessage.deserialize("You can always open the tutorial again by running "))
                    .append(Component.text("/c tutorial", NamedTextColor.YELLOW)
                            .hoverEvent(HoverEvent.showText(Component.text("Click to open the tutorial!")))
                            .clickEvent(ClickEvent.runCommand("/c tutorial")))

            );
            UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
                event.getPlayer().openBook(TutorialSubCommand.book);
            }, 20L);

        }
    }
}
