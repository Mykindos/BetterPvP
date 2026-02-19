package me.mykindos.betterpvp.clans.clans.leveling.ceremony;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.leveling.events.ClanLevelUpEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * Orchestrates the level-up ceremony when {@link ClanLevelUpEvent} fires.
 *
 * <p>Handles:
 * <ul>
 *   <li>Chat broadcast to all online clan members</li>
 *   <li>Title popup on all online clan members' screens</li>
 *   <li>Sound effect played to all online members</li>
 *   <li>Particle effects spawned at the clan core (if set)</li>
 *   <li>Server-wide broadcast on milestone levels (configurable interval)</li>
 * </ul>
 *
 * <p>Any future system that needs to react to level-ups (achievement checks, clan logs,
 * Discord webhooks, etc.) should register its own listener for {@link ClanLevelUpEvent}
 * instead of modifying this class.
 */
@BPvPListener
@Singleton
public class ClanLevelUpService implements Listener {

    @Inject
    @Config(path = "clans.leveling.ceremony.milestoneInterval", defaultValue = "10")
    private int milestoneInterval;

    @Inject
    private ClientManager clientManager;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(ClanLevelUpEvent event) {
        Clan clan = event.getClan();
        long newLevel = event.getNewLevel();

        List<Player> online = clan.getMembersAsPlayers();

        // 1. Chat message
        clan.sendMessage(Component.empty()
                .append(Component.text("LEVEL UP!", NamedTextColor.GOLD, TextDecoration.BOLD))
                .appendSpace()
                .append(Component.text("Your clan has reached level ", NamedTextColor.YELLOW))
                .append(Component.text(String.format("%,d", newLevel), NamedTextColor.GREEN))
                .append(Component.text("!", NamedTextColor.YELLOW)));

        // 2. Title popup on all online members
        final TextComponent title = Component.text("Level Up!", NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
        final TextComponent subTitle = Component.text(clan.getName() + " is now level " + String.format("%,d", newLevel) + "!", NamedTextColor.YELLOW);
        for (Player player : online) {
            final Gamer gamer = clientManager.search().online(player).getGamer();
            gamer.getTitleQueue().add(200, new TitleComponent(
                    0.25,
                    1.0,
                    0.25,
                    false,
                    gmr -> title,
                    gmr -> subTitle
            ));
        }

        // 3. Sound effect
        new SoundEffect(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f).play(clan);

        // 4. Server-wide broadcast on milestone levels
        if (milestoneInterval > 0 && newLevel % milestoneInterval == 0) {
            Bukkit.broadcast(Component.empty()
                    .append(Component.text(clan.getName(), NamedTextColor.GOLD))
                    .append(Component.text(" reached level ", NamedTextColor.YELLOW))
                    .append(Component.text(String.format("%,d", newLevel), NamedTextColor.GREEN))
                    .append(Component.text("!", NamedTextColor.YELLOW)));
        }
    }

}
