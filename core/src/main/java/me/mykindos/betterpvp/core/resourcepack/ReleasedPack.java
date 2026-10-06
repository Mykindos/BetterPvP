package me.mykindos.betterpvp.core.resourcepack;

import lombok.Value;
import net.kyori.adventure.resource.ResourcePackInfo;

import java.net.URI;
import java.util.UUID;

/**
 * One pack of a release. Its id is the same in every release, so a client keeps one copy per id and downloads it
 * again only when the sha1 changes.
 */
@Value
public class ReleasedPack {

    String name;
    UUID id;
    String url;
    String sha1;
    long size;

    public ResourcePackInfo toInfo() {
        return ResourcePackInfo.resourcePackInfo(id, URI.create(url), sha1);
    }

}
