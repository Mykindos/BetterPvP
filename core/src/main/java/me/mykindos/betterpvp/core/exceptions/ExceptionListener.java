package me.mykindos.betterpvp.core.exceptions;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.discord.DiscordMessage;
import me.mykindos.betterpvp.core.discord.DiscordWebhook;
import me.mykindos.betterpvp.core.discord.embeds.EmbedObject;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.awt.Color;

@BPvPListener
public class ExceptionListener implements Listener {

    @Inject
    @Config(path = "core.exceptions.discord-webhook")
    private String discordWebhook;

    private final Core core;

    @Inject
    public ExceptionListener(Core core) {
        this.core = core;
    }

    @EventHandler
    public void onServerError(ServerExceptionEvent event) {

        UtilServer.runTask(core, true, () -> {

            String error = ExceptionUtils.getStackTrace(event.getException().getCause());

            DiscordWebhook webhook = new DiscordWebhook(discordWebhook);
            webhook.send(DiscordMessage.builder().username("Error")
                    .messageContent("An error has occurred on the server!")
                    .embed(EmbedObject.builder().description(error.substring(0, Math.min(800, error.length()))).color(Color.RED).build())
                    .build());
        });


    }

}
