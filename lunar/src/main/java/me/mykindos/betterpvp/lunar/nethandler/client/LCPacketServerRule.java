package me.mykindos.betterpvp.lunar.nethandler.client;

import lombok.Getter;
import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.client.obj.ServerRule;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;

import java.io.IOException;

public final class LCPacketServerRule extends LCPacket {

    @Getter private ServerRule rule;
    @Getter private int intValue;
    @Getter private float floatValue;
    @Getter private boolean booleanValue;
    @Getter private String stringValue = "";

    public LCPacketServerRule() {}

    public LCPacketServerRule(ServerRule rule, float value) {
        this.rule = rule;
        this.floatValue = value;
    }

    public LCPacketServerRule(ServerRule rule, boolean value) {
        this.rule = rule;
        this.booleanValue = value;
    }

    public LCPacketServerRule(ServerRule rule, int value) {
        this.rule = rule;
        this.intValue = value;
    }

    public LCPacketServerRule(ServerRule rule, String value) {
        this.rule = rule;
        this.stringValue = value;
    }

    @Override
    public void write(ByteBufWrapper buf) throws IOException {
        buf.writeString(rule.getId());
        buf.buf().writeBoolean(booleanValue);
        buf.buf().writeInt(intValue);
        buf.buf().writeFloat(floatValue);
        buf.writeString(stringValue);
    }

    @Override
    public void read(ByteBufWrapper buf) throws IOException {
        this.rule = ServerRule.getRule(buf.readString());
        this.booleanValue = buf.buf().readBoolean();
        this.intValue = buf.buf().readInt();
        this.floatValue = buf.buf().readFloat();
        this.stringValue = buf.readString();
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleServerRule(this);
    }

}