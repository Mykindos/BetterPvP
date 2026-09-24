package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.hall.Steward;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.settler.morale.MoraleBoost;
import me.mykindos.betterpvp.core.world.settler.morale.MoraleEngine;
import me.mykindos.betterpvp.core.world.settler.presence.SettlerPresence;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Great Hall upgrade: once per cooldown a member rings the bell, which lifts every settler's morale for a while, sends
 * the settlers in the camp to stand by the Steward for a moment and sounds for every member in the camp. When it was
 * last rung is kept on the camp record, and the lift lasts from then.
 */
@BPvPListener
@Singleton
public class GreatBell implements Listener, MoraleBoost {

    public static final String ID = "great_bell";

    private final CampUpgrades upgrades;
    private final CampConfig config;
    private final CampStore store;
    private final MoraleEngine moraleEngine;
    private final Provider<SettlerPresence> presence;
    private final StructureShapes shapes;
    private final SiteInstances instances;
    private final ClanManager clanManager;
    private final LongSupplier clock;

    @Inject
    public GreatBell(@NotNull CampUpgrades upgrades, @NotNull CampConfig config, @NotNull CampStore store,
                     @NotNull MoraleEngine moraleEngine, @NotNull Provider<SettlerPresence> presence,
                     @NotNull StructureShapes shapes, @NotNull SiteInstances instances,
                     @NotNull ClanManager clanManager) {
        this(upgrades, config, store, moraleEngine, presence, shapes, instances, clanManager,
                System::currentTimeMillis);
    }

    GreatBell(@NotNull CampUpgrades upgrades, @NotNull CampConfig config, @NotNull CampStore store,
              @NotNull MoraleEngine moraleEngine, @NotNull Provider<SettlerPresence> presence,
              @NotNull StructureShapes shapes, @NotNull SiteInstances instances, @NotNull ClanManager clanManager,
              @NotNull LongSupplier clock) {
        this.upgrades = upgrades;
        this.config = config;
        this.store = store;
        this.moraleEngine = moraleEngine;
        this.presence = presence;
        this.shapes = shapes;
        this.instances = instances;
        this.clanManager = clanManager;
        this.clock = clock;
        upgrades.declare(CampConstruction.GREAT_HALL, ID, 3);
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampConstruction.GREAT_HALL, ID);
    }

    @Override
    public int morale(@NotNull SiteKey site) {
        return isActive(site) && liftLeft(site) > 0 ? bonus() : 0;
    }

    /** The morale ringing gives every settler while it lasts. */
    public int bonus() {
        return numbers().map(numbers -> numbers.setting("morale", 10)).orElse(10);
    }

    /** How long the lift from one ring lasts. */
    public @NotNull Duration length() {
        return Duration.ofMinutes(numbers().map(numbers -> numbers.setting("minutes", 120)).orElse(120));
    }

    /** How long a camp waits between rings. */
    public @NotNull Duration cooldown() {
        return Duration.ofMinutes(Math.round(numbers().map(numbers -> numbers.setting("cooldown-hours", 24.0))
                .orElse(24.0) * 60));
    }

    /** How long the lift from camp {@code key}'s last ring has left, or 0 when it has worn off. */
    public long liftLeft(@NotNull SiteKey key) {
        return sinceRung(key, length());
    }

    /** How long until camp {@code key} can ring again, or 0 if it can now. */
    public long readyIn(@NotNull SiteKey key) {
        return sinceRung(key, cooldown());
    }

    private long sinceRung(@NotNull SiteKey key, @NotNull Duration window) {
        return store.cached(key.getOwnerId())
                .filter(camp -> camp.getBellRungAt() > 0)
                .map(camp -> Math.max(0, camp.getBellRungAt() + window.toMillis() - clock.getAsLong()))
                .orElse(0L);
    }

    /**
     * Rings the bell of {@code hall} in camp {@code key}.
     *
     * @return the translation key of why it was refused, or null once rung
     */
    public @Nullable String ring(@NotNull Player player, @NotNull SiteKey key, @NotNull PlacedStructure hall) {
        final String refused = ring(key);
        if (refused == null) {
            call(player, key, hall);
        }
        return refused;
    }

    /** Marks camp {@code key}'s bell as rung and lifts its settlers, or returns why it could not be rung. */
    @Nullable String ring(@NotNull SiteKey key) {
        if (store.cached(key.getOwnerId()).isEmpty()) {
            return "core.settler.not_loaded";
        }
        if (!isActive(key)) {
            return "clans.camp.upgrade.great_bell.inactive";
        }
        if (readyIn(key) > 0) {
            return "clans.camp.upgrade.great_bell.spent";
        }
        store.cached(key.getOwnerId()).ifPresent(camp -> camp.setBellRungAt(clock.getAsLong()));
        store.changed(key.getOwnerId());
        moraleEngine.settle(key);
        return null;
    }

    /** Sends the camp's settlers to the Steward and sounds the bell for the members in the camp's world. */
    private void call(@NotNull Player ringer, @NotNull SiteKey key, @NotNull PlacedStructure hall) {
        final World world = ringer.getWorld();
        final boolean inCamp = instances.byWorld(world.getName())
                .map(SiteInstance::getKey)
                .filter(key::equals)
                .isPresent();
        if (!inCamp) {
            return;
        }
        final Location spot = shapes.point(world, hall, Steward.POINT)
                .orElseGet(() -> hall.getPosition().toLocation(world).add(0.5, 1, 0.5));
        presence.get().gather(key, world, spot);

        final Optional<Clan> clan = clanManager.getClanById(key.getOwnerId());
        final Component message = Translations.component("clans.camp.upgrade.great_bell.rung",
                Component.text(ringer.getName(), NamedTextColor.YELLOW)).color(NamedTextColor.GRAY);
        for (Player player : world.getPlayers()) {
            if (clan.flatMap(found -> found.getMemberByUUID(player.getUniqueId())).isEmpty()) {
                continue;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.5f, 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 1f, 1f);
            UtilMessage.message(player, Translations.component("clans.prefix.camp"), message);
        }
    }

    private @NotNull Optional<CampConfig.UpgradeNumbers> numbers() {
        return config.upgrade(CampConstruction.GREAT_HALL, ID);
    }
}
