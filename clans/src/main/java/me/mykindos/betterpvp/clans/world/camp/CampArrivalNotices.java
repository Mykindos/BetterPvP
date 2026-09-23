package me.mykindos.betterpvp.clans.world.camp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.settler.SettlerDeparture;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.construction.StructureStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tells a member arriving at their camp what is waiting for them: structures ready to claim, structures that need
 * repairing, are disabled or wait for a crew, settlers on strike, and settlers who left in the last day. Nothing is
 * said when nothing is waiting.
 */
@BPvPListener
@Singleton
public class CampArrivalNotices implements Listener {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private final Clans clans;
    private final Camps camps;
    private final ConstructionService construction;
    private final StructureCatalogue catalogue;
    private final SettlerService settlers;

    @Inject
    public CampArrivalNotices(@NotNull Clans clans, @NotNull Camps camps, @NotNull ConstructionService construction,
                              @NotNull StructureCatalogue catalogue, @NotNull SettlerService settlers) {
        this.clans = clans;
        this.camps = camps;
        this.construction = construction;
        this.catalogue = catalogue;
        this.settlers = settlers;
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        arrive(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        arrive(event.getPlayer());
    }

    private void arrive(@NotNull Player player) {
        // The world's content is still being laid out on the tick a player arrives, so the holding may not be ready.
        UtilServer.runTaskLater(clans, () -> {
            if (player.isOnline() && camps.isMember(player, player.getWorld())) {
                notices(player.getWorld()).forEach(line -> UtilMessage.message(player, "clans.prefix.camp", line));
            }
        }, 40L);
    }

    /** One line per structure waiting on the clan, in the order they are held. */
    public @NotNull List<Component> notices(@NotNull World world) {
        final List<Component> lines = new ArrayList<>();
        final long now = construction.now();
        construction.worksite(world).ifPresent(worksite -> {
            for (PlacedStructure structure : worksite.getHolding().getStructures()) {
                final String key = switch (structure.status(now)) {
                    case READY_TO_CLAIM -> "clans.camp.notice.ready";
                    case NEEDS_REPAIR -> "clans.camp.notice.needs_repair";
                    case DISABLED -> "clans.camp.notice.disabled";
                    case PAUSED -> "clans.camp.notice.needs_crew";
                    default -> null;
                };
                if (key == null) {
                    continue;
                }
                final Component name = catalogue.find(structure.getType())
                        .map(type -> type.getDisplayName())
                        .orElseGet(() -> Component.text(structure.getType()));
                final NamedTextColor colour = structure.status(now) == StructureStatus.READY_TO_CLAIM
                        ? NamedTextColor.GREEN : NamedTextColor.RED;
                lines.add(Translations.component(key, name.color(NamedTextColor.YELLOW)).color(colour));
            }
            settlers.roster(worksite.getKey()).ifPresent(roster -> {
                final int striking = roster.inState(SettlerState.STRIKING).size();
                if (striking > 0) {
                    lines.add(Translations.component("clans.camp.notice.striking", Component.text(striking))
                            .color(NamedTextColor.RED));
                }
                for (SettlerDeparture departure : roster.getDepartures()) {
                    if (now - departure.getAt() <= DAY_MILLIS) {
                        lines.add(Translations.component("clans.camp.notice.left."
                                        + departure.getReason().name().toLowerCase(Locale.ROOT),
                                Component.text(departure.getName(), departure.getRarity().getColor()))
                                .color(NamedTextColor.GRAY));
                    }
                }
            });
        });
        return lines;
    }
}
