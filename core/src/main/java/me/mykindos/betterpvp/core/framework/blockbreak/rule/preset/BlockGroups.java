package me.mykindos.betterpvp.core.framework.blockbreak.rule.preset;

import me.mykindos.betterpvp.core.framework.blockbreak.rule.matcher.TagMatcher;
import org.bukkit.Tag;

/**
 * Statically declared {@link BlockGroup} presets, built from Bukkit's vanilla
 * material tags so they stay in sync with new blocks added in future versions.
 * <p>
 * These are the "STONES / WOODS / DIRTS" canonical groups requested by the spec.
 * Add new groups here as the project needs them.
 */
public final class BlockGroups {

    private BlockGroups() {}

    public static final BlockGroup STONES = new BlockGroup(
            "stones", new TagMatcher(Tag.MINEABLE_PICKAXE));

    public static final BlockGroup WOODS = new BlockGroup(
            "woods", new TagMatcher(Tag.MINEABLE_AXE));

    public static final BlockGroup DIRTS = new BlockGroup(
            "dirts", new TagMatcher(Tag.MINEABLE_SHOVEL));

    public static final BlockGroup LEAVES = new BlockGroup(
            "leaves", new TagMatcher(Tag.LEAVES));

    public static final BlockGroup LOGS = new BlockGroup(
            "logs", new TagMatcher(Tag.LOGS));

    public static final BlockGroup CROPS = new BlockGroup(
            "crops", new TagMatcher(Tag.CROPS));

    public static final BlockGroup WOOL = new BlockGroup(
            "wool", new TagMatcher(Tag.WOOL));

    public static final BlockGroup SAND = new BlockGroup(
            "sand", new TagMatcher(Tag.SAND));
}
