package me.mykindos.betterpvp.core.item.impl.cannon.model;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectFactoryManager;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Makes cannons spawnable through the generic scene tooling ({@code /npc spawn cannon <archetype>}) and supplies
 * {@link CannonProp} with the factory reference every scene entity carries. Each archetype id is a spawnable type.
 */
@Singleton
@PluginAdapter(value = "ModelEngine", loadMethodName = "ignored")
public class CannonFactory extends SceneObjectFactory {

    private final Provider<CannonService> service;
    private final CannonArchetypeRegistry archetypes;

    @Inject
    private CannonFactory(SceneObjectRegistry registry, SceneObjectFactoryManager factoryManager,
                          CannonArchetypeRegistry archetypes, Provider<CannonService> service) {
        super("cannon", registry);
        this.archetypes = archetypes;
        this.service = service;
        factoryManager.addObject("cannon", this);
    }

    @Override
    public String[] getTypes() {
        return archetypes.ids();
    }

    @Override
    public SceneObject spawnDefault(@NotNull Location location, @NotNull String type) {
        // Command-spawned cannons are player-placed as far as persistence is concerned: they must still be there after
        // a restart, because nothing else would re-create them.
        return service.get().spawn(null, location, type, CannonProperties.normal(), true);
    }
}
