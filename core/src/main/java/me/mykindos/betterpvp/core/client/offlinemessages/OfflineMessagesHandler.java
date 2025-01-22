package me.mykindos.betterpvp.core.client.offlinemessages;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.offlinemessages.menu.OfflineMessagesMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.description.Describable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

@Singleton
public class OfflineMessagesHandler {
    private final OfflineMessagesRepository offlineMessagesRepository;

    @Inject
    public OfflineMessagesHandler(OfflineMessagesRepository offlineMessagesRepository) {
        this.offlineMessagesRepository = offlineMessagesRepository;
    }

    /**
     * Handles a client logging in and sending them their offline messages
     * @param client the client logging in
     * @param player the player logging in
     */
    public void onLogin(Client client, Player player) {
        offlineMessagesRepository.getNewOfflineMessagesForClient(client).thenAcceptAsync((offlineMessages) -> {
            if (offlineMessages.isEmpty()) return;

            UtilMessage.message(player, "Offline", "While you were away, you received <green>%s</green> Offline Messages", offlineMessages.size());
            for (int i = 0; i < 10; i++) {
                if (i >= offlineMessages.size()) break;
                offlineMessages.get(i).send();

            }
            Component additional = Component.empty().append(Component.text("To read all outstanding messages "))
                    .append(Component.text("Click Here", NamedTextColor.WHITE).decoration(TextDecoration.UNDERLINED, true).clickEvent(ClickEvent.callback(audience ->
                    {
                        Player runner = (Player) audience;
                        getOfflineMessagesMenu(runner.getName(), offlineMessages)
                                .show(runner);
                    }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build()))).appendSpace()
                    .append(Component.text(" or use "))
                    .append(Component.text("/offlinemessages <time> <unit>", NamedTextColor.YELLOW).clickEvent(ClickEvent.suggestCommand("/offlinemessages 7 d"))).appendSpace()
                    .append(Component.text("to retrieve past messages for the given time duration"));
            UtilMessage.message(player, "Offline", additional);
        });


    }

    /**
     * Shows the relevant OfflineMessagesMenu to the player
     * @param player the player this menu is being shown to
     * @param name the name of the player this menu is about
     * @param id the id of the player this menu is about
     * @param time how far back to see messages
     */
    public void showMenuForMessagesForClientAfterTime(Player player, String name, UUID id, long time) {
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> {
            List<OfflineMessage> messages = offlineMessagesRepository.getOfflineMessagesForClient(id, time).join();
            UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
                getOfflineMessagesMenu(name, messages).show(player);
            });
        });
    }

    /**
     * Creates the OfflineMessagesMenu comprising the OfflineMessages
     * @param name the name of the player this menu is about
     * @param messages the OfflineMessages
     * @return The OfflineMessageMenu
     */
    private OfflineMessagesMenu getOfflineMessagesMenu(String name, List<OfflineMessage> messages) {
        return new OfflineMessagesMenu(name + "'s Offline Messages", messages.stream()
                .map(Describable.class::cast).toList(), null);
    }

    /**
     * Store an OfflineMessage for the player
     * @param id the id of the player
     * @param action the type of OfflineMessage this is
     * @param message the message, mini-message formatted
     * @param args optional args
     */
    public void sendOfflineMessage(UUID id, OfflineMessage.Action action, String message, Object... args) {
        String finalMessage = String.format(message, args);
        OfflineMessage offlineMessage = new OfflineMessage(id, System.currentTimeMillis(), action, finalMessage);
        offlineMessagesRepository.save(offlineMessage);
    }

}
