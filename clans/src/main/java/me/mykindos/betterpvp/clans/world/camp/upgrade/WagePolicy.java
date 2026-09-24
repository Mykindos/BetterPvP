package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.settler.wage.CoinAccount;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Great Hall upgrade: members who chip in pay what the wage fund cannot, so settlers only strike once they cannot
 * either. What is short is split evenly between the contributors who are online, each paying at most the configured
 * {@code daily-cap} coins a real day, counted in UTC days.
 * <p>
 * It works by standing behind the wage fund: {@link #backing(CoinAccount)} is the account payroll draws from, holding
 * the fund plus what contributors could still pay today.
 */
@Singleton
public class WagePolicy {

    public static final String ID = "wage_policy";

    private static final long DAY_MILLIS = 86_400_000L;

    private final CampUpgrades upgrades;
    private final CampStore store;
    private final CampConfig config;
    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @Inject
    public WagePolicy(@NotNull CampUpgrades upgrades, @NotNull CampStore store, @NotNull CampConfig config,
                      @NotNull ClanManager clanManager, @NotNull ClientManager clientManager) {
        this.upgrades = upgrades;
        this.store = store;
        this.config = config;
        this.clanManager = clanManager;
        this.clientManager = clientManager;
        upgrades.declare(CampConstruction.GREAT_HALL, ID, 2);
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampConstruction.GREAT_HALL, ID);
    }

    /** The most one member pays toward wages in a day. */
    public long dailyCap() {
        return config.upgrade(CampConstruction.GREAT_HALL, ID).map(numbers -> numbers.setting("daily-cap", 500))
                .orElse(500);
    }

    public boolean contributes(@NotNull SiteKey key, @NotNull UUID member) {
        return store.cached(key.getOwnerId()).map(camp -> camp.getWageContributors().contains(member)).orElse(false);
    }

    /** Starts or stops {@code player} chipping in, for a member of the camp's clan. */
    public void toggle(@NotNull Player player, @NotNull SiteKey key) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        if (camp == null || !isMember(key, player.getUniqueId())) {
            return;
        }
        if (!camp.getWageContributors().remove(player.getUniqueId())) {
            camp.getWageContributors().add(player.getUniqueId());
        }
        store.changed(key.getOwnerId());
    }

    /** Coins {@code member} has paid toward wages today. */
    public long paidToday(@NotNull SiteKey key, @NotNull UUID member) {
        return store.cached(key.getOwnerId())
                .filter(camp -> camp.getWageContributionDay() == today())
                .map(camp -> camp.getWageContributions().getOrDefault(member, 0L))
                .orElse(0L);
    }

    /** The account payroll draws on: {@code fund} first, then contributors while the upgrade works. */
    public @NotNull CoinAccount backing(@NotNull CoinAccount fund) {
        return new CoinAccount() {
            @Override
            public long balance(@NotNull SiteKey site) {
                return fund.balance(site) + room(site).values().stream().mapToLong(Long::longValue).sum();
            }

            @Override
            public void withdraw(@NotNull SiteKey site, long amount) {
                final long fromFund = Math.min(amount, fund.balance(site));
                fund.withdraw(site, fromFund);
                if (amount > fromFund) {
                    cover(site, amount - fromFund);
                }
            }
        };
    }

    /** What each online contributor to camp {@code key} could still pay today. */
    private @NotNull Map<UUID, Long> room(@NotNull SiteKey key) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        if (camp == null || camp.getWageContributors().isEmpty() || !isActive(key)) {
            return Map.of();
        }
        final Map<UUID, Long> balances = new LinkedHashMap<>();
        for (UUID member : camp.getWageContributors()) {
            final Player player = Bukkit.getPlayer(member);
            if (player != null && isMember(key, member)) {
                balances.put(member, (long) gamer(player).getBalance());
            }
        }
        return room(camp, balances, dailyCap(), today());
    }

    private void cover(@NotNull SiteKey key, long amount) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        if (camp == null) {
            return;
        }
        final Map<UUID, Long> shares = split(amount, room(key));
        if (shares.isEmpty()) {
            return;
        }
        startDay(camp, today());
        shares.forEach((member, share) -> {
            final Player player = Bukkit.getPlayer(member);
            if (player == null || share <= 0) {
                return;
            }
            final Gamer gamer = gamer(player);
            gamer.saveProperty(GamerProperty.BALANCE, gamer.getBalance() - (int) (long) share);
            camp.getWageContributions().merge(member, share, Long::sum);
            UtilMessage.message(player, Translations.component("clans.prefix.camp"),
                    Translations.component("clans.camp.upgrade.wage_policy.paid",
                            Component.text(UtilFormat.formatNumber((int) (long) share), NamedTextColor.GOLD))
                            .color(NamedTextColor.GRAY));
        });
        store.changed(key.getOwnerId());
    }

    /**
     * What each member could still pay today: the lesser of their balance and what the daily cap leaves them. Anyone
     * who could pay nothing is left out.
     */
    static @NotNull Map<UUID, Long> room(@NotNull Camp camp, @NotNull Map<UUID, Long> balances, long cap, long day) {
        final Map<UUID, Long> room = new LinkedHashMap<>();
        balances.forEach((member, balance) -> {
            final long paid = camp.getWageContributionDay() == day ? camp.getWageContributions().getOrDefault(member, 0L) : 0;
            final long left = Math.min(balance, cap - paid);
            if (left > 0) {
                room.put(member, left);
            }
        });
        return room;
    }

    /**
     * Splits {@code amount} as evenly as {@code room} allows. Whoever cannot pay a full share pays what they can, and
     * the rest is spread over the others. The shares add up to less than {@code amount} only when there is not room
     * for all of it.
     */
    static @NotNull Map<UUID, Long> split(long amount, @NotNull Map<UUID, Long> room) {
        final List<Map.Entry<UUID, Long>> payers = new ArrayList<>(room.entrySet());
        payers.sort(Map.Entry.comparingByValue());
        final Map<UUID, Long> shares = new LinkedHashMap<>();
        long left = amount;
        for (int i = 0; i < payers.size() && left > 0; i++) {
            final long remaining = payers.size() - i;
            final long share = Math.min(payers.get(i).getValue(), (left + remaining - 1) / remaining);
            if (share > 0) {
                shares.put(payers.get(i).getKey(), share);
                left -= share;
            }
        }
        return shares;
    }

    /** Clears what was paid on an earlier day. */
    static void startDay(@NotNull Camp camp, long day) {
        if (camp.getWageContributionDay() != day) {
            camp.getWageContributions().clear();
            camp.setWageContributionDay(day);
        }
    }

    private long today() {
        return System.currentTimeMillis() / DAY_MILLIS;
    }

    private boolean isMember(@NotNull SiteKey key, @NotNull UUID member) {
        return clanManager.getClanById(key.getOwnerId())
                .flatMap(clan -> clan.getMemberByUUID(member))
                .isPresent();
    }

    private @NotNull Gamer gamer(@NotNull Player player) {
        return clientManager.search().online(player).getGamer();
    }
}
