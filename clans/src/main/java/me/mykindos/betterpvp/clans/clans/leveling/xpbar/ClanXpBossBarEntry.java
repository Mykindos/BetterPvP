package me.mykindos.betterpvp.clans.clans.leveling.xpbar;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the transient boss bar state for a single clan's XP notifications.
 * Not persisted — recreated on demand when XP is gained.
 */
@Getter
@Setter
public class ClanXpBossBarEntry {

    private final BossBar bar;
    /** Tracks which players are currently seeing this bar so we can cleanly remove them. */
    private final Set<Player> viewingPlayers = ConcurrentHashMap.newKeySet();
    /** Accumulated XP displayed since the bar last appeared. Reset when the bar is hidden. */
    private double pendingXp;
    /** Timestamp of the most recent XP gain. Used to determine when to fade the bar. */
    private long lastActivityMillis;
    /** Whether the bar is currently being shown to online clan members. */
    private boolean visible;

    public ClanXpBossBarEntry() {
        this.bar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        this.pendingXp = 0;
        this.lastActivityMillis = System.currentTimeMillis();
        this.visible = false;
    }

    public void addViewer(Player player) {
        bar.addViewer(player);
        viewingPlayers.add(player);
    }

    public void removeViewer(Player player) {
        bar.removeViewer(player);
        viewingPlayers.remove(player);
    }

    /** Removes all tracked viewers from the bar and clears the set. */
    public void clearViewers() {
        for (Player player : viewingPlayers) {
            bar.removeViewer(player);
        }
        viewingPlayers.clear();
    }

    public void accumulate(double xp) {
        this.pendingXp += xp;
        this.lastActivityMillis = System.currentTimeMillis();
    }

    public boolean isExpired(long timeoutMs) {
        return System.currentTimeMillis() - lastActivityMillis > timeoutMs;
    }

}
