package me.mykindos.betterpvp.proxy.resourcepack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.kyori.adventure.resource.ResourcePackInfo;

import java.net.URI;
import java.util.UUID;

/**
 * One pack of a release. Its id is the same in every release, so a client keeps one copy per id and downloads it
 * again only when the sha1 changes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ReleasedPack {

    private String name;
    private UUID id;
    private String url;
    private String sha1;

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }

    public String getSha1() {
        return sha1;
    }

    public ResourcePackInfo toInfo() {
        return ResourcePackInfo.resourcePackInfo(id, URI.create(url), sha1);
    }

}
