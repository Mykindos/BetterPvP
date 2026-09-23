package me.mykindos.betterpvp.clans.world.camp.hall;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.settler.SettlerLeaveReason;
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

/** Tells a camp's online members when its settlers strike over wages, go back to work, or leave unpaid. */
@BPvPListener
@Singleton
public class WageNotices implements Listener {

    private final ClanManager clanManager;

    @Inject
    public WageNotices(@NotNull ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStrike(@NotNull SettlerStrikeEvent event) {
        tell(event.getSite(), Translations.component(event.isStriking()
                        ? "clans.camp.hall.wages.strike_started" : "clans.camp.hall.wages.strike_ended",
                Component.text(event.getSettlers().size()))
                .color(event.isStriking() ? NamedTextColor.RED : NamedTextColor.GREEN));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeft(@NotNull SettlerLeftEvent event) {
        if (event.getReason() == SettlerLeaveReason.UNPAID) {
            tell(event.getSite(), Translations.component("clans.camp.hall.wages.left_unpaid",
                    Component.text(event.getSettler().getName(), event.getSettler().getRarity().getColor()))
                    .color(NamedTextColor.RED));
        }
    }

    private void tell(@NotNull SiteKey site, @NotNull Component message) {
        if (!site.getSiteId().equals(Camps.SITE_ID)) {
            return;
        }
        clanManager.getClanById(site.getOwnerId()).ifPresent(clan -> clan.getMembers().forEach(member -> {
            final Player player = Bukkit.getPlayer(member.getUuid());
            if (player != null) {
                UtilMessage.message(player, Translations.component("clans.prefix.camp"), message);
            }
        }));
    }
}
