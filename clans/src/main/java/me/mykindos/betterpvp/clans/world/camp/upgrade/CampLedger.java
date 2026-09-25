package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.resource.ResourceKind;
import me.mykindos.betterpvp.clans.world.camp.resource.ResourcesDepositedEvent;
import me.mykindos.betterpvp.clans.world.camp.settler.WageFundPaidEvent;
import me.mykindos.betterpvp.clans.world.camp.settler.recruit.SettlerHiredEvent;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructure;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.StructureClaimedEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

/**
 * Great Hall upgrade: the camp ledger, a page listing who deposited resources, claimed jobs, hired settlers and paid
 * into the wage fund, and when. Entries are recorded on the camp whether or not the ledger is fitted, so fitting it
 * shows what came before. The camp keeps the newest {@code max-entries} of them.
 */
@BPvPListener
@Singleton
public class CampLedger implements Listener {

    public static final String ID = "camp_ledger";

    private final CampStore store;
    private final CampUpgrades upgrades;
    private final CampConfig config;
    private final CampStructures structures;
    private final LongSupplier clock;

    @Inject
    public CampLedger(@NotNull CampStore store, @NotNull CampUpgrades upgrades, @NotNull CampConfig config,
                      @NotNull CampStructures structures) {
        this(store, upgrades, config, structures, System::currentTimeMillis);
    }

    CampLedger(@NotNull CampStore store, @NotNull CampUpgrades upgrades, @NotNull CampConfig config,
               @NotNull CampStructures structures, @NotNull LongSupplier clock) {
        this.store = store;
        this.upgrades = upgrades;
        this.config = config;
        this.structures = structures;
        this.clock = clock;
        upgrades.declare(CampConstruction.GREAT_HALL, ID, 1);
        upgrades.page(ID, (player, camp, structure, previous) -> {
            if (!isActive(camp)) {
                UtilMessage.plain(player, Translations.component("clans.camp.upgrade.camp_ledger.inactive").color(NamedTextColor.GRAY));
                return;
            }
            new CampLedgerMenu(this, camp, previous).show(player);
        });
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampConstruction.GREAT_HALL, ID);
    }

    /** Camp {@code key}'s entries, newest first. */
    public @NotNull List<LedgerEntry> newestFirst(@NotNull SiteKey key) {
        return store.cached(key.getOwnerId())
                .map(camp -> newestFirst(camp.getLedger()))
                .orElse(List.of());
    }

    static @NotNull List<LedgerEntry> newestFirst(@NotNull List<LedgerEntry> ledger) {
        return List.copyOf(ledger).reversed();
    }

    /** Adds {@code entry} to the end of {@code ledger}, dropping the oldest entries past {@code limit}. */
    static void append(@NotNull List<LedgerEntry> ledger, @NotNull LedgerEntry entry, int limit) {
        ledger.add(entry);
        final int excess = ledger.size() - Math.max(0, limit);
        if (excess > 0) {
            ledger.subList(0, excess).clear();
        }
    }

    void record(@NotNull SiteKey key, @NotNull Player member, @NotNull LedgerEntry.Kind kind,
                @NotNull List<String> args) {
        if (!key.getSiteId().equals(Camps.SITE_ID)) {
            return;
        }
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        if (camp == null) {
            return;
        }
        final int limit = config.upgrade(CampConstruction.GREAT_HALL, ID)
                .map(numbers -> numbers.setting("max-entries", 200))
                .orElse(200);
        append(camp.getLedger(), new LedgerEntry(clock.getAsLong(), member.getUniqueId(), member.getName(), kind,
                new ArrayList<>(args)), limit);
        store.changed(key.getOwnerId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeposit(@NotNull ResourcesDepositedEvent event) {
        final List<String> args = new ArrayList<>();
        event.getAmounts().forEach((kind, amount) -> {
            args.add(kind.id());
            args.add(String.valueOf(amount));
        });
        record(event.getSite(), event.getPlayer(), LedgerEntry.Kind.DEPOSIT, args);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClaim(@NotNull StructureClaimedEvent event) {
        final Job job = event.getJob();
        final List<String> args = new ArrayList<>(List.of(job.getKind().name(), event.getStructure().getType()));
        if (job.getKind() == JobKind.ADVANCE) {
            args.add(String.valueOf(job.getTargetStage()));
        } else if (job.getKind() == JobKind.FIT_UPGRADE && job.getUpgrade() != null) {
            args.add(job.getUpgrade());
        }
        record(event.getSite(), event.getPlayer(), LedgerEntry.Kind.CLAIM, args);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHire(@NotNull SettlerHiredEvent event) {
        record(event.getSite(), event.getPlayer(), LedgerEntry.Kind.HIRE,
                List.of(event.getSettler().getName(), String.valueOf(event.getPrice())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWages(@NotNull WageFundPaidEvent event) {
        record(event.getSite(), event.getPlayer(), LedgerEntry.Kind.WAGES, List.of(String.valueOf(event.getAmount())));
    }

    /** What {@code entry} says happened, in the reader's language. */
    public @NotNull Component describe(@NotNull LedgerEntry entry) {
        final List<String> args = entry.getArgs();
        final String prefix = "clans.camp.upgrade.camp_ledger.entry.";
        return switch (entry.getKind()) {
            case DEPOSIT -> {
                final List<Component> parts = new ArrayList<>();
                for (int i = 0; i + 1 < args.size(); i += 2) {
                    final String amount = args.get(i + 1);
                    ResourceKind.byId(args.get(i)).ifPresent(kind -> parts.add(Translations.component(
                            "clans.camp.resource.amount", Component.text(amount), kind.displayName())));
                }
                yield Translations.component(prefix + "deposit", Component.join(JoinConfiguration.commas(true), parts));
            }
            case CLAIM -> claim(args);
            case HIRE -> Translations.component(prefix + "hire", Component.text(arg(args, 0), NamedTextColor.WHITE),
                    coins(arg(args, 1)));
            case WAGES -> Translations.component(prefix + "wages", coins(arg(args, 0)));
        };
    }

    private @NotNull Component claim(@NotNull List<String> args) {
        final String type = arg(args, 1);
        final CampStructure structure = structures.find(type).orElse(null);
        final Component name = structure == null ? Component.text(type) : structure.getDisplayName();
        final String kind = arg(args, 0).toLowerCase(Locale.ROOT);
        final String key = "clans.camp.upgrade.camp_ledger.entry.claim." + kind;
        if (kind.equals("advance")) {
            final int stage = parse(arg(args, 2));
            return Translations.component(key, name,
                    structure == null ? Component.text(stage + 1) : structure.stageName(stage));
        }
        if (kind.equals("fit_upgrade")) {
            return Translations.component(key, name,
                    Translations.component("clans.camp.upgrade." + arg(args, 2) + ".name"));
        }
        return Translations.component(key, name);
    }

    private static @NotNull Component coins(@NotNull String amount) {
        return Component.text(UtilFormat.formatNumber(parse(amount)), NamedTextColor.GOLD);
    }

    private static @NotNull String arg(@NotNull List<String> args, int index) {
        return index < args.size() ? args.get(index) : "";
    }

    private static int parse(@NotNull String number) {
        try {
            return (int) Long.parseLong(number);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
