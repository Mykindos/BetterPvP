package me.mykindos.betterpvp.core.scene.behavior;

import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.Core;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Scene behavior base class for handling ModelEngine script keyframes on one model.
 * Works on any scene entity (NPC or prop) that holds an {@link ActiveModel}.
 */
public abstract class ModelEngineScriptBehavior implements SceneBehavior, ModelEngineScriptHandler {

    private final ActiveModel model;
    private final ModelEngineScriptDispatcher dispatcher;

    protected ModelEngineScriptBehavior(ActiveModel model) {
        this(model, JavaPlugin.getPlugin(Core.class).getInjector().getInstance(ModelEngineScriptDispatcher.class));
    }

    private ModelEngineScriptBehavior(ActiveModel model, ModelEngineScriptDispatcher dispatcher) {
        this.model = model;
        this.dispatcher = dispatcher;
    }

    @Override
    public ActiveModel getModel() {
        return model;
    }

    @Override
    public void start() {
        dispatcher.register(this);
    }

    @Override
    public void stop() {
        dispatcher.unregister(this);
    }

    @Override
    public void tick() {
    }

}
