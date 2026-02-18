package me.mykindos.betterpvp.clans.clans.leveling.xpbar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.clans.clans.leveling.ClanExperience;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages transient XP boss bars shown to clan members when XP is gained.
 *
 * <p>One bar per clan is created lazily on first XP gain and automatically faded
 * after {@code clans.leveling.xpbar.fadeDelayMs} milliseconds of inactivity.
 * Bars are never persisted — they are recreated on the next XP event after a server restart.
 */
@BPvPListener
@Singleton
@CustomLog
public class ClanXpBossBarService implements Listener {

    /** Keyed by clan DB id. Only contains clans with an active or recently-active bar. */
    private final ConcurrentHashMap<Long, ClanXpBossBarEntry> entries = new ConcurrentHashMap<>();

    private final ClanManager clanManager;

    @Inject
    @Config(path = "clans.leveling.xpbar.fadeDelayMs", defaultValue = "5000")
    private long fadeDelayMs;

    @Inject
    public ClanXpBossBarService(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    /**
     * Called by {@link me.mykindos.betterpvp.clans.clans.listeners.ClanExperienceListener}
     * whenever XP is granted to a clan.
     */
    public void notifyXpGain(Clan clan, double amount, String reason) {
        ClanXpBossBarEntry entry = entries.computeIfAbsent(clan.getId(), id -> new ClanXpBossBarEntry());
        entry.accumulate(amount);

        if (!entry.isVisible()) {
            clan.getMembers().stream()
                    .map(ClanMember::getPlayer)
                    .filter(Objects::nonNull)
                    .forEach(entry::addViewer);
            entry.setVisible(true);
        }

        updateBarDisplay(clan, entry);
    }

    /** Ticks every 250 ms — updates display and fades expired bars. */
    @UpdateEvent(delay = 250)
    public void tick() {
        entries.forEach((clanId, entry) -> {
            if (entry.isExpired(fadeDelayMs)) {
                if (entry.isVisible()) {
                    hideBar(entry);
                }
                entries.remove(clanId);
                return;
            }

            clanManager.getClanById(clanId).ifPresentOrElse(
                    clan -> updateBarDisplay(clan, entry),
                    () -> {
                        hideBar(entry);
                        entries.remove(clanId);
                    }
            );
        });
    }

    private void updateBarDisplay(Clan clan, ClanXpBossBarEntry entry) {
        long level = clan.getExperience().getLevel();
        double xpIn = ClanExperience.xpInCurrentLevel(level, clan.getExperience().getXp());
        double xpNeeded = ClanExperience.xpRequiredForNextLevel(level);
        float progress = (float) Math.min(1.0, xpIn / Math.max(1, xpNeeded));

        Component name = Component.text("Level ", NamedTextColor.GRAY)
                .append(Component.text(level, NamedTextColor.YELLOW))
                .append(Component.text("  +", NamedTextColor.GREEN))
                .append(Component.text(String.format("%,.1f XP", entry.getPendingXp()), NamedTextColor.GREEN))
                .append(Component.text("  (", NamedTextColor.GRAY))
                .append(Component.text(String.format("%,.0f / %,.0f", xpIn, xpNeeded), NamedTextColor.WHITE))
                .append(Component.text(")", NamedTextColor.GRAY));

        entry.getBar().name(name);
        entry.getBar().progress(progress);
    }

    private void hideBar(ClanXpBossBarEntry entry) {
        entry.clearViewers();
        entry.setVisible(false);
        entry.setPendingXp(0);
    }

    // --- Viewer lifecycle management ---

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        clanManager.getClanByPlayer(event.getPlayer()).ifPresent(clan -> {
            ClanXpBossBarEntry entry = entries.get(clan.getId());
            if (entry != null && entry.isVisible()) {
                entry.addViewer(event.getPlayer());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        clanManager.getClanByPlayer(event.getPlayer()).ifPresent(clan -> {
            ClanXpBossBarEntry entry = entries.get(clan.getId());
            if (entry != null) {
                entry.removeViewer(event.getPlayer());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMemberLeave(MemberLeaveClanEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        ClanXpBossBarEntry entry = entries.get(event.getClan().getId());
        if (entry != null) {
            entry.removeViewer(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDisband(ClanDisbandEvent event) {
        ClanXpBossBarEntry entry = entries.remove(event.getClan().getId());
        if (entry != null) {
            hideBar(entry);
        }
    }

}
