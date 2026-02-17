package me.mykindos.betterpvp.clans.clans.leveling.ceremony;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.leveling.events.ClanLevelUpEvent;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(ClanLevelUpEvent event) {
        Clan clan = event.getClan();
        long newLevel = event.getNewLevel();

        List<Player> online = clan.getMembers().stream()
                .map(ClanMember::getPlayer)
                .filter(Objects::nonNull)
                .toList();

        // 1. Chat message
        clan.messageClan(
                "<gold><bold>LEVEL UP!</bold> <yellow>Your clan has reached level <green>"
                        + newLevel + "<yellow>!",
                null, false
        );

        // 2. Title popup on all online members
        Title title = Title.title(
                Component.text("Level Up!", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text(clan.getName() + " is now level " + newLevel + "!", NamedTextColor.YELLOW),
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(2500),
                        Duration.ofMillis(500)
                )
        );
        online.forEach(p -> p.showTitle(title));

        // 3. Sound effect
        online.forEach(p -> p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f));

        // 4. Particles at clan core
        Location corePos = clan.getCore().getPosition();
        if (corePos != null && corePos.getWorld() != null) {
            Location center = corePos.clone().add(0.5, 1.5, 0.5);
            corePos.getWorld().spawnParticle(Particle.END_ROD,    center, 60, 0.5, 0.5, 0.5, 0.15);
            corePos.getWorld().spawnParticle(Particle.FIREWORK,   center, 40, 1.0, 1.0, 1.0, 0.05);
            corePos.getWorld().playSound(corePos, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
        }

        // 5. Server-wide broadcast on milestone levels
        if (milestoneInterval > 0 && newLevel % milestoneInterval == 0) {
            Bukkit.broadcast(
                    Component.text("[Clans] ", NamedTextColor.GOLD)
                            .append(Component.text(clan.getName(), NamedTextColor.YELLOW))
                            .append(Component.text(" reached level ", NamedTextColor.WHITE))
                            .append(Component.text(newLevel, NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                            .append(Component.text("!", NamedTextColor.WHITE))
            );
        }
    }

}
