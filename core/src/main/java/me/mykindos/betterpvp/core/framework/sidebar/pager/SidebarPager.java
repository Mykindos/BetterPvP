package me.mykindos.betterpvp.core.framework.sidebar.pager;

import com.google.common.collect.Iterators;
import lombok.NonNull;
import me.mykindos.betterpvp.core.framework.sidebar.Sidebar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class SidebarPager {

    private final List<Sidebar> sidebars;
    private final Iterator<Sidebar> pageIterator;
    private final Set<UUID> viewers;
    private final BukkitTask switchTask;
    private Sidebar currentPage;

    /**
     * Creates a new sidebar pager.
     *
     * @param sidebars         - list of sidebars to use
     * @param switchDelayTicks - delay between page switches in ticks (if value is 0, pages will not be switched automatically)
     * @param plugin           - plugin instance
     */
    public SidebarPager(@NonNull List<Sidebar> sidebars, long switchDelayTicks, @NonNull Plugin plugin) {
        this.sidebars = sidebars;
        this.viewers = new HashSet<>();
        this.pageIterator = Iterators.cycle(sidebars);
        this.currentPage = pageIterator.next();

        if (switchDelayTicks > 0) {
            this.switchTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::switchPage, switchDelayTicks, switchDelayTicks);
        } else {
            this.switchTask = null;
        }
    }

    public void applyToAll(Consumer<Sidebar> consumer) {
        sidebars.forEach(consumer);
    }

    /**
     * Switches to the next page.
     * Note: this method is called automatically by the scheduler.
     */
    public void switchPage() {
        currentPage.removeViewers();

        currentPage = pageIterator.next();

        for (UUID viewer : viewers) {
            Player player = Bukkit.getPlayer(viewer);
            if (player != null) {
                currentPage.addViewer(player);
            }
        }
    }

    public Sidebar getCurrentPage() {
        return currentPage;
    }

    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    public List<Sidebar> getSidebars() {
        return Collections.unmodifiableList(sidebars);
    }

    /**
     * Adds a page status line to all sidebars in pager.
     */
    public void addPageLine(PageConsumer consumer) {
        int page = 1;
        int maxPage = sidebars.size();

        for (Sidebar sidebar : sidebars) {
            consumer.accept(page, maxPage, sidebar);
            page++;
        }
    }

    /**
     * Destroy all sidebars in pager.
     * Note: pager object will be unusable after this method call.
     */
    public void destroy() {
        if (switchTask != null) {
            switchTask.cancel();
        }
        for (Sidebar sidebar : sidebars) {
            sidebar.destroy();
        }
        sidebars.clear();
        viewers.clear();
    }

    /**
     * Start showing all sidebars in pager to the player.
     *
     * @param player - player to show sidebars to
     */
    public void show(@NonNull Player player) {
        synchronized (viewers) {
            viewers.add(player.getUniqueId());
        }
        currentPage.addViewer(player);
    }

    /**
     * Stop showing all sidebars in pager to the player.
     *
     * @param player - player to stop showing sidebars to
     */
    public void hide(@NonNull Player player) {
        synchronized (viewers) {
            viewers.remove(player.getUniqueId());
        }
        currentPage.removeViewer(player);
    }
}
