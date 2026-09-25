package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ChatHint;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructurePlacedEvent;
import me.mykindos.betterpvp.core.world.construction.StructureStatus;
import me.mykindos.betterpvp.core.world.construction.StructureStatusChangeEvent;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.settler.crew.CrewRule;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/** Tells members in the camp when a job starts waiting for a crew, which the Steward in the Great Hall can staff. */
@BPvPListener
@Singleton
public class CrewNotices implements Listener {

    private final Camps camps;
    private final SiteInstances instances;
    private final StructureCatalogue catalogue;
    private final CrewRule rule;

    @Inject
    public CrewNotices(@NotNull Camps camps, @NotNull SiteInstances instances, @NotNull StructureCatalogue catalogue,
                       @NotNull CrewRule rule) {
        this.camps = camps;
        this.instances = instances;
        this.catalogue = catalogue;
        this.rule = rule;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlaced(@NotNull StructurePlacedEvent event) {
        announce(event.getSite(), event.getStructure());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStatusChange(@NotNull StructureStatusChangeEvent event) {
        if (event.getTo() == StructureStatus.PAUSED) {
            announce(event.getSite(), event.getStructure());
        }
    }

    private void announce(@NotNull SiteKey site, @NotNull PlacedStructure structure) {
        final Job job = structure.getJob();
        if (!site.getSiteId().equals(Camps.SITE_ID) || job == null || !job.getHolds().contains(CrewRule.ID)) {
            return;
        }
        final Component name = catalogue.find(structure.getType())
                .map(StructureType::getDisplayName)
                .orElseGet(() -> Component.text(structure.getType()));
        final Component message = ChatHint.INFO.attach(
                Translations.component("clans.settler.crew.needed", name.color(NamedTextColor.YELLOW),
                        Component.text(rule.threshold(structure, job), NamedTextColor.YELLOW)).color(NamedTextColor.GRAY),
                Translations.component("clans.settler.crew.needed_hint").color(NamedTextColor.GRAY));

        for (SiteInstance instance : instances.forKey(site)) {
            final World world = Bukkit.getWorld(instance.getWorldName());
            if (world == null) {
                continue;
            }
            for (Player player : world.getPlayers()) {
                if (camps.isMember(player, world)) {
                    UtilMessage.message(player, Translations.component("clans.prefix.settler"), message);
                }
            }
        }
    }
}
