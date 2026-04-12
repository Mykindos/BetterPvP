package me.mykindos.betterpvp.core.scene.behavior;

import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.model.ActiveModel;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Global ModelEngine script keyframe dispatcher.
 * <p>
 * ModelEngine script readers are registered globally, so this class owns the single
 * {@value #READER_ID} reader and delegates scripts to handlers registered for the
 * specific {@link ActiveModel} that produced the animation keyframe.
 */
@CustomLog
@Singleton
@PluginAdapter("ModelEngine")
public class ModelEngineScriptDispatcher {

    public static final String READER_ID = "betterpvp";

    private final Map<ActiveModel, Set<ModelEngineScriptHandler>> handlers = new IdentityHashMap<>();

    public void load() {
        ModelEngineAPI.getAPI().getScriptReaderRegistry().register(READER_ID, this::dispatch);
        log.info("Registered ModelEngine script reader '{}'", READER_ID).submit();
    }

    public void register(ModelEngineScriptHandler handler) {
        synchronized (handlers) {
            handlers.computeIfAbsent(handler.getModel(), ignored -> new HashSet<>()).add(handler);
        }
    }

    public void unregister(ModelEngineScriptHandler handler) {
        synchronized (handlers) {
            final Set<ModelEngineScriptHandler> modelHandlers = handlers.get(handler.getModel());
            if (modelHandlers == null) {
                return;
            }

            modelHandlers.remove(handler);
            if (modelHandlers.isEmpty()) {
                handlers.remove(handler.getModel());
            }
        }
    }

    private void dispatch(IAnimationProperty property, String script) {
        final Set<ModelEngineScriptHandler> modelHandlers;
        synchronized (handlers) {
            final Set<ModelEngineScriptHandler> registered = handlers.get(property.getModel());
            if (registered == null || registered.isEmpty()) {
                return;
            }
            modelHandlers = Set.copyOf(registered);
        }

        for (ModelEngineScriptHandler handler : modelHandlers) {
            if (handler.acceptsScript(property, script)) {
                handler.onScript(property, script);
            }
        }
    }

}
