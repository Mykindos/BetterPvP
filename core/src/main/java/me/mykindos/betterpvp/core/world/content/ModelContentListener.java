package me.mykindos.betterpvp.core.world.content;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import com.ticxo.modelengine.api.generator.ModelGenerator;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Tells {@link WorldContentService} each time ModelEngine finishes registering models, which covers both the first
 * load and every {@code /meg reload}.
 */
@BPvPListener
@Singleton
@PluginAdapter("Mapper")
@PluginAdapter("ModelEngine")
public class ModelContentListener implements Listener {

    private final Core core;
    private final WorldContentService contentService;

    @Inject
    public ModelContentListener(@NotNull Core core, @NotNull WorldContentService contentService) {
        this.core = core;
        this.contentService = contentService;
    }

    @EventHandler
    public void onModelsRegistered(@NotNull ModelRegistrationEvent event) {
        if (event.getPhase() == ModelGenerator.Phase.FINISHED) {
            UtilServer.runTask(core, contentService::modelsReady);
        }
    }
}
