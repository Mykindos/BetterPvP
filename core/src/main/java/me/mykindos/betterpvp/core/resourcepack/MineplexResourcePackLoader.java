package me.mykindos.betterpvp.core.resourcepack;

import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.resourcepack.ResourcePackModule;
import lombok.CustomLog;

import java.util.Optional;

@CustomLog
public class MineplexResourcePackLoader implements IResourcePackLoader {

    @Override
    public ResourcePack loadResourcePack(String name) {
        ResourcePackModule resourcePackModule = MineplexModuleManager.getRegisteredModule(ResourcePackModule.class);

        Optional<com.mineplex.studio.sdk.modules.resourcepack.ResourcePack> resourcePackOptional = resourcePackModule.get(name);
        if (resourcePackOptional.isEmpty()) {
            log.error("Failed to load resource pack " + name).submit();
            throw new RuntimeException("Failed to get resource pack from studio SDK");
        }

        com.mineplex.studio.sdk.modules.resourcepack.ResourcePack resourcePack = resourcePackOptional.get();

        return new ResourcePack(resourcePack.getUuid(), resourcePack.getUrl(), resourcePack.getSha1());
    }

}
