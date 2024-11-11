package me.mykindos.betterpvp.core.framework.blocktag;

import lombok.Data;
import org.bukkit.block.Block;

import java.util.UUID;

@Data
public class BlockTag {

    private final Block block;
    private final UUID tagger;
    private final BlockTagType tagType;


    public enum BlockTagType {
        TAG,
        UNTAG
    }

}
