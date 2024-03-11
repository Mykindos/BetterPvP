package me.mykindos.betterpvp.core.framework.sidebar.protocol;

import lombok.Getter;


public enum PacketIds {

    UPDATE_TEAMS(0x5E),
    UPDATE_SCORE(0x5F),
    OBJECTIVE_DISPLAY(0x55),
    OBJECTIVE(0x5C);

    @Getter
    private final int packetId;

    PacketIds(int packetId) {
        this.packetId = packetId;
    }

}
