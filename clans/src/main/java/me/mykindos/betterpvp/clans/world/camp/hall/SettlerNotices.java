package me.mykindos.betterpvp.clans.world.camp.hall;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.recruit.SettlerBoatEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ChatHint;
import me.mykindos.betterpvp.core.utilities.model.ChatIcon;
import me.mykindos.betterpvp.core.world.settler.SettlerLeftEvent;
import me.mykindos.betterpvp.core.world.settler.wage.SettlerStrikeEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Tells a camp's online members when settlers arrive at the Dock, strike over wages, go back to work, or leave unpaid
 * or unhappy.
 */
@BPvPListener
@Singleton
public class SettlerNotices implements Listener {

    private final ClanManager clanManager;

    @Inject
    public SettlerNotices(@NotNull ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStrike(@NotNull SettlerStrikeEvent event) {
        final int count = event.getSettlers().size();
        if (!event.isStriking()) {
            tell(event.getSite(), ChatIcon.NEWS.line(Translations.component("clans.camp.hall.wages.strike_ended",
                    Component.text(count, NamedTextColor.WHITE)).color(NamedTextColor.YELLOW)));
            return;
        }
        tell(event.getSite(), ChatIcon.PROBLEM.line(ChatHint.INFO.attach(
                Translations.component("clans.camp.hall.wages.strike_started",
                        Component.text(count, NamedTextColor.YELLOW)).color(NamedTextColor.RED),
                Translations.component("clans.camp.hall.wages.strike_hint").color(NamedTextColor.GRAY))));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBoat(@NotNull SettlerBoatEvent event) {
        tell(event.getSite(), ChatIcon.NEWS.line(Translations.component(event.isMilestone()
                ? "clans.settler.recruit.milestone_arrived" : "clans.settler.recruit.boat_arrived")
                .color(NamedTextColor.YELLOW)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeft(@NotNull SettlerLeftEvent event) {
        final String key = switch (event.getReason()) {
            case UNPAID -> "clans.camp.hall.wages.left_unpaid";
            case UNHAPPY -> "clans.camp.hall.settlers.left_unhappy";
            case DISMISSED -> null;
        };
        if (key != null) {
            tell(event.getSite(), ChatIcon.PROBLEM.line(Translations.component(key,
                    Component.text(event.getSettler().getName(), event.getSettler().getRarity().getColor()))
                    .color(NamedTextColor.RED)));
        }
    }

    private void tell(@NotNull SiteKey site, @NotNull Component message) {
        if (!site.getSiteId().equals(Camps.SITE_ID)) {
            return;
        }
        clanManager.getClanById(site.getOwnerId()).ifPresent(clan -> clan.getMembers().forEach(member -> {
            final Player player = Bukkit.getPlayer(member.getUuid());
            if (player != null) {
                UtilMessage.plain(player, message);
            }
        }));
    }
}
