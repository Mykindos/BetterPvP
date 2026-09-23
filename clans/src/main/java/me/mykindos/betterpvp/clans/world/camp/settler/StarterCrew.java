package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.settler.SettlerGenerator;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerTemplate;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Gives a camp the settlers every camp starts with, once, the first time its world opens with a Great Hall standing.
 * Without them a new camp could not staff its first job. Runs after the starting structures are placed.
 */
@Singleton
@CustomLog
public class StarterCrew implements WorldContent {

    /** The history source starting settlers are rolled with. */
    public static final String SOURCE = "starting";

    private final CampStore store;
    private final SettlerService service;
    private final SettlerGenerator generator;
    private final SettlerConfig config;
    private final SiteInstances instances;

    @Inject
    public StarterCrew(@NotNull CampStore store, @NotNull SettlerService service, @NotNull SettlerGenerator generator,
                       @NotNull SettlerConfig config, @NotNull SiteInstances instances) {
        this.store = store;
        this.service = service;
        this.generator = generator;
        this.config = config;
        this.instances = instances;
    }

    @Override
    public void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
        instances.byWorld(world.getName()).map(SiteInstance::getKey).ifPresent(this::grant);
    }

    void grant(@NotNull SiteKey key) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        if (camp == null || camp.isStartingSettlers() || service.populationCap(key) <= 0) {
            return;
        }

        for (SettlerConfig.Starter starter : config.getStartingSettlers()) {
            final SettlerTemplate template = SettlerTemplate.builder()
                    .rarity(starter.getRarity())
                    .profession(starter.getProfession())
                    .source(SOURCE)
                    .build();
            final SettlerResult result = service.grant(key,
                    generator.roll(template, config.getTable(), ThreadLocalRandom.current()));
            if (!result.isSuccess()) {
                log.warn("Could not give camp {} a starting settler", key).submit();
            }
        }
        camp.setStartingSettlers(true);
        store.changed(key.getOwnerId());
    }
}
