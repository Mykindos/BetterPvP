package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Value;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.settler.prosperity.CampProsperity;
import me.mykindos.betterpvp.clans.world.camp.settler.prosperity.ProsperityStanding;
import me.mykindos.betterpvp.clans.world.camp.settler.prosperity.ProsperityStore;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Great Hall upgrade: the rival board, a page ranking this camp among the camps closest to it by Prosperity, the
 * configured number above and below it, each with how its Prosperity changed since its daily snapshot. Camps are read
 * from the {@link ProsperityStore}, and this camp's own Prosperity is the live one.
 */
@BPvPListener
@Singleton
@CustomLog
public class RivalBoard implements Listener {

    public static final String ID = "rival_board";

    private final Clans clans;
    private final CampUpgrades upgrades;
    private final CampConfig config;
    private final ProsperityStore store;
    private final CampProsperity prosperity;
    private final ClanManager clanManager;

    @Inject
    public RivalBoard(@NotNull Clans clans, @NotNull CampUpgrades upgrades, @NotNull CampConfig config,
                      @NotNull ProsperityStore store, @NotNull CampProsperity prosperity,
                      @NotNull ClanManager clanManager) {
        this.clans = clans;
        this.upgrades = upgrades;
        this.config = config;
        this.store = store;
        this.prosperity = prosperity;
        this.clanManager = clanManager;
        upgrades.declare(CampConstruction.GREAT_HALL, ID, 3);
        upgrades.page(ID, (player, camp, structure, previous) -> open(player, camp, previous));
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampConstruction.GREAT_HALL, ID);
    }

    /** Reads the standings off the main thread, then shows the board. */
    public void open(@NotNull Player player, @NotNull SiteKey key, @Nullable Windowed previous) {
        if (!isActive(key)) {
            UtilMessage.plain(player, Translations.component("clans.camp.upgrade.rival_board.inactive").color(NamedTextColor.GRAY));
            return;
        }
        final int own = prosperity.of(key);
        final CampConfig.UpgradeNumbers numbers = config.upgrade(CampConstruction.GREAT_HALL, ID).orElse(null);
        final int above = numbers == null ? 5 : numbers.setting("above", 5);
        final int below = numbers == null ? 5 : numbers.setting("below", 5);
        UtilServer.runTaskAsync(clans, () -> {
            final Map<Long, ProsperityStanding> standings;
            try {
                standings = store.standings();
            } catch (Exception ex) {
                log.error("Could not read Prosperity standings", ex).submit();
                return;
            }
            final List<Row> rows = rank(standings, key.getOwnerId(), own, above, below);
            UtilServer.runTask(clans, () -> {
                if (player.isOnline()) {
                    new RivalBoardMenu(this, rows, previous).show(player);
                }
            });
        });
    }

    /**
     * Ranks every camp by Prosperity, highest first with ties broken by clan id, and keeps {@code own} and up to
     * {@code above} camps ranked just over it and {@code below} just under it. The own camp counts at {@code ownNow},
     * measured against its snapshot if it has one.
     */
    static @NotNull List<Row> rank(@NotNull Map<Long, ProsperityStanding> standings, long own, int ownNow,
                                   int above, int below) {
        final Map<Long, ProsperityStanding> all = new HashMap<>(standings);
        final ProsperityStanding recorded = all.get(own);
        all.put(own, new ProsperityStanding(ownNow, recorded == null ? ownNow : recorded.getSnapshot()));

        final List<Map.Entry<Long, ProsperityStanding>> ordered = new ArrayList<>(all.entrySet());
        ordered.sort(Comparator.<Map.Entry<Long, ProsperityStanding>>comparingInt(entry -> -entry.getValue().getProsperity())
                .thenComparingLong(Map.Entry::getKey));

        int position = 0;
        while (ordered.get(position).getKey() != own) {
            position++;
        }
        final int from = Math.max(0, position - Math.max(0, above));
        final int to = Math.min(ordered.size(), position + Math.max(0, below) + 1);
        final List<Row> rows = new ArrayList<>();
        for (int i = from; i < to; i++) {
            final Map.Entry<Long, ProsperityStanding> entry = ordered.get(i);
            rows.add(new Row(i + 1, entry.getKey(), entry.getValue().getProsperity(), entry.getValue().change(),
                    entry.getKey() == own));
        }
        return rows;
    }

    @NotNull Optional<String> clanName(long clanId) {
        return clanManager.getClanById(clanId).map(Clan::getName);
    }

    /** One camp on the board. */
    @Value
    public static class Row {
        /** Its place among every camp, 1 being the most prosperous. */
        int rank;
        long clanId;
        int prosperity;
        int change;
        boolean own;
    }
}
