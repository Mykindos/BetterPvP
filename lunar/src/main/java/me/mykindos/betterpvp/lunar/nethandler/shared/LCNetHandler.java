package me.mykindos.betterpvp.lunar.nethandler.shared;

public interface LCNetHandler {

    void handleAddWaypoint(LCPacketWaypointAdd packet);
    void handleRemoveWaypoint(LCPacketWaypointRemove packet);
    void handleEmote(LCPacketEmoteBroadcast packet);

}