package me.mykindos.betterpvp.core.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.chat.ChatModule;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

@PluginAdapter("StudioEngine")
@Singleton
@BPvPListener
public class MineplexChatListener implements Listener {

    private final Core core;
    private final ChatModule chatModule;

    @Inject
    public MineplexChatListener(Core core) {
        this.core = core;
        chatModule = MineplexModuleManager.getRegisteredModule(ChatModule.class);
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onChatMessageSent(ChatSentEvent event) {
        // TODO wait for mineplex to expose a way to do this..
    }

    @EventHandler
    public void onSignEdit(final SignChangeEvent event) {
        // Since a sign is essentially one message, we want to filter all its lines together.
        final StringBuilder messageBuilder = new StringBuilder();
        for (final Component line : event.lines()) {
            // Since Signs can have component lines, we have to serialize each line into plain text to filter it.
            messageBuilder.append(' ').append(PlainTextComponentSerializer.plainText().serialize(line));
        }
        // Since the SignChangeEvent is fired on the main thread, we have to check the filter asynchronously.
        chatModule.isFilteredAsync(messageBuilder.toString()).thenAccept(filtered -> {
                    // If the line wasn't filtered, we don't need to do anything
                    if (Boolean.TRUE.equals(filtered)) {
                       UtilServer.runTask(core, () -> {
                            // Update the sign to clear its lines
                            if (event.getBlock().getState() instanceof Sign sign) {
                                SignSide side = sign.getSide(event.getSide());
                                final Component cleared = Component.text("");
                                // Minecraft signs have only 4 lines
                                for (int i = 0; i < 4; i++) {
                                    side.line(i, cleared);
                                }
                                // We want to update the sign without triggering a game physics update
                                sign.update(false, false);
                            }
                        });
                    }
                });
    }

}
