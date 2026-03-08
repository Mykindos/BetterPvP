package me.mykindos.betterpvp.core.resourcepack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Singleton
@CustomLog
@Getter
public class ResourcePackHandler {

    private final HashMap<String, ResourcePack> resourcePacks = new HashMap<>();
    private IResourcePackLoader resourcePackLoader;

    public CompletableFuture<ResourcePack> getResourcePack(String name) {
        if (resourcePacks.containsKey(name)) {
            return CompletableFuture.completedFuture(resourcePacks.get(name));
        }
        return resourcePackLoader.loadResourcePack(name).thenApply(
                resourcePack -> {
                    resourcePacks.put(name, resourcePack);
                    return resourcePack;
                }
        );
    }

    @Inject
    public ResourcePackHandler(Core core) {

        this.resourcePackLoader = new DefaultResourcePackLoader(core);

    }

    public void reload() {
        resourcePacks.clear();
    }

}
