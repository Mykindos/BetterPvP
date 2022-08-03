package me.mykindos.betterpvp.lunar.nethandler.server;

import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketClientVoice;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketVoiceChannelSwitch;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketVoiceMute;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;

public interface LCNetHandlerServer extends LCNetHandler {

    void handleStaffModStatus(LCPacketStaffModStatus packet);
    void handleVoice(LCPacketClientVoice packet);
    void handleVoiceMute(LCPacketVoiceMute packet);
    void handleVoiceChannelSwitch(LCPacketVoiceChannelSwitch packet);
}