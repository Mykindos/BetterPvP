package me.mykindos.betterpvp.clans.world.camp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.settler.SettlerDeparture;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Tells a member arriving at their camp what is waiting for them. Warnings come first, marked with an exclamation mark
 * in red: structures that need repairing, are disabled or wait for a crew, and settlers on strike. Notices follow,
 * marked with a bell in yellow: structures ready to claim and settlers who left in the last day. A single bell rings
 * when there is at least one line. Joining and the world changes that follow it all count as one arrival. Nothing is
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
    private final Map<UUID, BukkitTask> pending = new HashMap<>();

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
        final UUID id = player.getUniqueId();
        final BukkitTask previous = pending.remove(id);
        if (previous != null) {
            previous.cancel();
        }
        // The world's content is still being laid out on the tick a player arrives, so the holding may not be ready.
        pending.put(id, UtilServer.runTaskLater(clans, () -> {
            pending.remove(id);
            if (player.isOnline() && camps.isMember(player, player.getWorld())) {
                announce(player, player.getWorld());
            }
        }, 40L));
    }

    private void announce(@NotNull Player player, @NotNull World world) {
        final List<Component> warnings = new ArrayList<>();
        final List<Component> notices = new ArrayList<>();
        final long now = construction.now();
        construction.worksite(world).ifPresent(worksite -> {
            for (PlacedStructure structure : worksite.getHolding().getStructures()) {
                final Component name = catalogue.find(structure.getType())
                        .map(type -> type.getDisplayName())
                        .orElseGet(() -> Component.text(structure.getType()))
                        .color(NamedTextColor.WHITE);
                switch (structure.status(now)) {
                    case NEEDS_REPAIR -> warnings.add(warning(Translations.component("clans.camp.notice.needs_repair", name)));
                    case DISABLED -> warnings.add(warning(Translations.component("clans.camp.notice.disabled", name)));
                    case PAUSED -> warnings.add(warning(Translations.component("clans.camp.notice.needs_crew", name)));
                    case READY_TO_CLAIM -> notices.add(notice(Translations.component("clans.camp.notice.ready", name)));
                    default -> {
                    }
                }
            }
            settlers.roster(worksite.getKey()).ifPresent(roster -> {
                final int striking = roster.inState(SettlerState.STRIKING).size();
                if (striking > 0) {
                    warnings.add(warning(Translations.component("clans.camp.notice.striking",
                            Component.text(striking))));
                }
                for (SettlerDeparture departure : roster.getDepartures()) {
                    if (now - departure.getAt() <= DAY_MILLIS) {
                        notices.add(notice(Translations.component("clans.camp.notice.left."
                                        + departure.getReason().name().toLowerCase(Locale.ROOT),
                                Component.text(departure.getName(), NamedTextColor.WHITE))));
                    }
                }
            });
        });

        if (warnings.isEmpty() && notices.isEmpty()) {
            return;
        }
        warnings.forEach(player::sendMessage);
        notices.forEach(player::sendMessage);
        player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1f, 0.8f);
    }

    private @NotNull Component warning(@NotNull Component line) {
        return iconLine("exclamation_mark_icon", line.color(NamedTextColor.RED));
    }

    private @NotNull Component notice(@NotNull Component line) {
        return iconLine("bell_icon", line.color(NamedTextColor.YELLOW));
    }

    private @NotNull Component iconLine(@NotNull String icon, @NotNull Component line) {
        final Component sprite = Component.object(ObjectContents.sprite(Key.key("blocks"),
                Key.key("betterpvp", "menu/icon/regular/" + icon)));
        return Component.join(JoinConfiguration.spaces(), sprite, line.decorate(TextDecoration.BOLD));
    }
}
