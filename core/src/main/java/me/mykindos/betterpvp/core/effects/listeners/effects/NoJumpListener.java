package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@Singleton
@BPvPListener
public class NoJumpListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        // Reset the player's jump strength on logout
        AttributeInstance noJumpAttribute = event.getPlayer().getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
        if(noJumpAttribute != null) {
            noJumpAttribute.setBaseValue(noJumpAttribute.getDefaultValue());
        }
    }

}
