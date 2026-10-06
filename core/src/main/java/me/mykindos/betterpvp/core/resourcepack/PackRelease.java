package me.mykindos.betterpvp.core.resourcepack;

import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * What one channel of the pack server serves: the packs in load order, lowest priority first, and the glyph map
 * those packs were built with.
 */
@Value
public class PackRelease {

    String version;
    List<ReleasedPack> packs;
    Map<String, ReleasedGlyph> glyphs;

}
