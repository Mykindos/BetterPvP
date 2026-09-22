package me.mykindos.betterpvp.core.item.impl.cannon.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonRecord;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonService;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonStore;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentBinding;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.content.WorldContentService;
import me.mykindos.betterpvp.core.world.content.WorldSelector;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Rebuilds the player-placed cannons of each world from {@link CannonStore} as that world loads, so cannons in unloaded
 * chunks are known about without being spawned and none can be lost because its chunk was cold at boot.
 */
@Singleton
@PluginAdapter("Mapper")
@PluginAdapter("ModelEngine")
public class CannonContent implements WorldContent {

    private final CannonStore store;
    private final CannonService service;

    @Inject
    private CannonContent(Core core, CannonStore store, CannonService service, WorldContentService contentService) {
        this.store = store;
        this.service = service;
        contentService.register(core, new WorldContentBinding(WorldSelector.any(), () -> List.of(this))
                .withRequiresModels(true));
    }

    /** The store is only read here. A world going away must never delete the cannons standing in it. */
    @Override
    public void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
        for (CannonRecord record : store.all()) {
            if (!record.getWorld().equals(world.getName())) {
                continue;
            }

            final CannonProp prop = service.restore(record);
            if (prop != null) {
                scope.adopt(prop);
            }
        }
    }
}
