package me.mykindos.betterpvp.core.framework.blocktag;

import lombok.Getter;

@Getter
public enum BlockTags {

    PLAYER_MANIPULATED("PlayerManipulated");

    private final String tag;

    BlockTags(String tag) {
        this.tag = tag;
    }
}
