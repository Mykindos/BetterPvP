package me.mykindos.betterpvp.core.resourcepack;

import java.util.concurrent.CompletableFuture;

public interface IResourcePackLoader {

    CompletableFuture<ResourcePack> loadResourcePack(String name);

}
