package me.mykindos.betterpvp.lunar.serverrule;

import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.lunar.LunarClientAPI;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketServerRule;
import me.mykindos.betterpvp.lunar.nethandler.client.obj.ServerRule;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class LunarClientAPIServerRule {

    // The reason this is a Map is so that hopefully we will only have 1 server rule packet
    // per ServerRule. It would be extremely weird behavior if multiple with the same type were sent.
    private final Map<ServerRule, LCPacketServerRule> CUSTOM_SERVER_RULES = new HashMap<>();

    /**
     * Set a server rule to a boolean value.
     * All current server rules (02/02/2021)
     * use a boolean value.
     *
     * @param rule The ServerRule with type of boolean.
     * @param value The value of the ServerRule.
     */
    public void setRule(ServerRule rule, Boolean value) {
        CUSTOM_SERVER_RULES.put(rule, new LCPacketServerRule(rule, value));
    }

    /**
     * Send all set server rules to player(s).
     * This will likely work best to be sent on join.
     *
     * If a server rule is updated, the players will need to be updated.
     * This is not an automatic process. The ideal usage would be to set
     * all server rules when the plugin loads, then when each player joins
     * send them all server rules.
     *
     * @param players The player(s) to get all the previously set server rules.
     */
    public void sendServerRule(Player... players) {
        if (CUSTOM_SERVER_RULES.isEmpty()) {
            return;
        }
        for (Player player : players) {
            for (LCPacketServerRule value : CUSTOM_SERVER_RULES.values()) {
                LunarClientAPI.getInstance().sendPacket(player, value);
            }
        }
    }
}
