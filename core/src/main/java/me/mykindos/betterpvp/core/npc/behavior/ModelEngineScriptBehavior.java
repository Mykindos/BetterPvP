package me.mykindos.betterpvp.core.npc.behavior;

import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.Core;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * NPC behavior base class for handling ModelEngine script keyframes on one model.
 */
public abstract class ModelEngineScriptBehavior implements NPCBehavior, ModelEngineScriptHandler {

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
