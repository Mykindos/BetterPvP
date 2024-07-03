package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@Singleton
public class ShowItemCommand extends Command {
    private final CooldownManager cooldownManager;
    private final Core core;

    @Inject
    public ShowItemCommand(CooldownManager cooldownManager, Core core) {
        this.cooldownManager = cooldownManager;
        this.core = core;
    }

    @Override
    public String getName() {
        return "showitem";
    }

    @Override
    public String getDescription() {
        return "Sends the current held item to chat";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(!cooldownManager.use(player, getName(), 15d, false, true)){
            UtilMessage.message(player, "Command", "You must wait some time between using this command again");
            return;
        }
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getItemMeta().hasDisplayName()) {
            Component messageComponent = Component.text("I am currently holding ").append(
                    Objects.requireNonNull(itemStack.getItemMeta().displayName()).hoverEvent(itemStack.asHoverEvent())
            );
            UtilServer.runTaskAsync(core, () -> UtilServer.callEvent(new ChatSentEvent(player, Bukkit.getOnlinePlayers(), UtilMessage.deserialize("<yellow>%s:</yellow>"), messageComponent)));
        }
    }
}
