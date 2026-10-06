package me.mykindos.betterpvp.proxy.resourcepack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The packs the pack server last published to a channel, read from the resource_pack_release table.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PackRelease {

    private String version;
    private List<ReleasedPack> packs = List.of();

    public String getVersion() {
        return version;
    }

    public List<ReleasedPack> getPacks() {
        return packs;
    }

}
