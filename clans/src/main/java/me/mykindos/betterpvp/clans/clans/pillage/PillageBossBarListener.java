package me.mykindos.betterpvp.clans.clans.pillage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageEndEvent;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageStartEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@Singleton
@BPvPListener
public class PillageBossBarListener implements Listener {

    @Inject
    private ClanManager clanManager;

    @Inject
    private Clans clans;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPillageStart(PillageStartEvent event) {
        final Pillage pillage = event.getPillage();
        pillage.getSiegedBar().addViewer(pillage.getPillaged().asAudience());
        pillage.getSiegingBar().addViewer(pillage.getPillager().asAudience());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPillageEnd(final PillageEndEvent event) {
        final Pillage pillage = event.getPillage();
        pillage.getSiegedBar().removeViewer(pillage.getPillaged().asAudience());
        pillage.getSiegingBar().removeViewer(pillage.getPillager().asAudience());
    }

    @UpdateEvent(delay = 1000)
    public void onTick() {
        this.clanManager.getPillageHandler().getActivePillages().forEach(Pillage::updateBossBar);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onJoin(final PlayerJoinEvent event) {
        final Clan clan = this.clanManager.getClanByPlayer(event.getPlayer()).orElse(null);
        addBars(clan, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onClanLeave(final MemberLeaveClanEvent event) {
        removeBars(event.getClan(), event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onClanJoin(final MemberJoinClanEvent event) {
        final Clan clan = event.getClan();
        addBars(clan, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onClanDisband(final ClanDisbandEvent event) {
        removeBars(event.getClan(), event.getClan().asAudience());
        for (Pillage pillage : this.clanManager.getPillageHandler().getPillagesBy(event.getClan())) {
            pillage.setPillageFinishTime(System.currentTimeMillis());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onKick(final ClanKickMemberEvent event) {
        if (event.getTarget().getGamer().isOnline()) {
            Player player = event.getTarget().getGamer().getPlayer();
            if (player == null) return;

            removeBars(event.getClan(), player);
        }
    }

    private void removeBars(Clan clan, Audience audience) {
        if (clan == null) {
            return;
        }

        for (Pillage pillage : this.clanManager.getPillageHandler().getPillagesOn(clan)) {
            pillage.getSiegedBar().removeViewer(audience);
        }

        for (Pillage pillage : this.clanManager.getPillageHandler().getPillagesBy(clan)) {
            pillage.getSiegingBar().removeViewer(audience);
        }
    }

    private void addBars(Clan clan, Audience audience) {
        if (clan == null) {
            return;
        }

        for (Pillage pillage : this.clanManager.getPillageHandler().getPillagesOn(clan)) {
            pillage.getSiegedBar().addViewer(audience);
        }

        for (Pillage pillage : this.clanManager.getPillageHandler().getPillagesBy(clan)) {
            pillage.getSiegingBar().addViewer(audience);
        }
    }
}
