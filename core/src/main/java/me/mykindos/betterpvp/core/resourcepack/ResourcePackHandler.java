package me.mykindos.betterpvp.core.resourcepack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
        if (Bukkit.getPluginManager().getPlugin("StudioEngine") == null) {
            this.resourcePackLoader = new DefaultResourcePackLoader(core);
        } else {
            try {
                Class<?> clazz = Class.forName("me.mykindos.betterpvp.core.resourcepack.MineplexResourcePackLoader");
                Constructor<?> constructor = clazz.getConstructor();
                this.resourcePackLoader = (IResourcePackLoader) constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException |
                     InvocationTargetException | NoSuchMethodException e) {
                log.error("Failed to load MineplexResourcePackLoader", e).submit();
            }
        }
    }

    public void reload() {
        resourcePacks.clear();
    }

}
