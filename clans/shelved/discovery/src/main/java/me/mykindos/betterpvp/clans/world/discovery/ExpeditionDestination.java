package me.mykindos.betterpvp.clans.world.discovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.crew.Crew;
import me.mykindos.betterpvp.clans.world.crew.CrewService;
import me.mykindos.betterpvp.clans.world.travel.Destination;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * "Out to sea" at the helm: the one course that names no port.
 * <p>
 * A {@link Destination} like anywhere else a ship can go, so the helm reuses the travel framework whole — the readiness
 * check, the guards that stop you leaving mid-fight, the departure hold. What differs is that arriving does not
 * teleport the clicker: it puts their whole crew on an expedition.
 */
@Singleton
public class ExpeditionDestination implements Destination {

    private final CrewService crewService;
    private final ExpeditionService expeditionService;

    @Inject
    public ExpeditionDestination(@NotNull CrewService crewService, @NotNull ExpeditionService expeditionService) {
        this.crewService = crewService;
        this.expeditionService = expeditionService;
    }

    @Override
    public @NotNull Key key() {
        return Key.key("betterpvp", "expedition");
    }

    @Override
    public @NotNull Component displayName() {
        return Component.translatable("clans.discovery.destination");
    }

    @Override
    public @NotNull ItemView icon() {
        return ItemView.builder().material(Material.COMPASS).build();
    }

    /** There is no port to check for; only whether an ocean can be handed out at all. */
    @Override
    public boolean isReady() {
        return expeditionService.canDepart();
    }

    /** Setting out is not arriving: they are welcomed nowhere, because they are going nowhere in particular. */
    @Override
    public boolean announcesArrival() {
        return false;
    }

    /**
     * Casts off. Only the captain reaches here — the helm refuses anyone else before the menu opens — and it is their
     * crew, not they alone, that leaves.
     */
    @Override
    public @NotNull CompletableFuture<Boolean> receive(@NotNull Player traveller) {
        final Optional<Crew> crew = crewService.captainedBy(traveller.getUniqueId());
        if (crew.isEmpty()) {
            UtilMessage.message(traveller, "clans.prefix.ship", "clans.ship.not-captain");
            return CompletableFuture.completedFuture(false);
        }

        return expeditionService.begin(crew.get());
    }
}
