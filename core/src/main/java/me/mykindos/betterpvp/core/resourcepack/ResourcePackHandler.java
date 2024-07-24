package me.mykindos.betterpvp.core.resourcepack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;

import java.util.HashMap;

@Singleton
@CustomLog
@Getter
public class ResourcePackHandler {

    private final HashMap<String, ResourcePack> resourcePacks = new HashMap<>();
    private IResourcePackLoader resourcePackLoader;

    public ResourcePack getResourcePack(String name) {
        return resourcePacks.computeIfAbsent(name, resourcePackLoader::loadResourcePack);
    }

    @Inject
    public ResourcePackHandler(Core core) {
        this.resourcePackLoader = new DefaultResourcePackLoader(core);
    }

    public void reload() {
        resourcePacks.clear();
    }

}
