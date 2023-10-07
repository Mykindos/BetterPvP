package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public class StaffChatCommand extends Command {

    private final GamerManager gamerManager;

    @Inject
    public StaffChatCommand(GamerManager gamerManager){
        this.gamerManager = gamerManager;

        aliases.add("sc");
    }

    @Override
    public String getName() {
        return "staffchat";
    }

    @Override
    public String getDescription() {
        return "Toggle staff only chat";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        if(gamerOptional.isPresent()) {
            if(args.length > 0) {
                String playerName = UtilFormat.spoofNameForLunar(player.getName());
                String message = "<dark_purple>" + playerName + " <light_purple>" + String.join(" ", args);

                gamerManager.sendMessageToRank("", message, Rank.HELPER);
                return;
            }
            boolean staffChatEnabled = true;
            Gamer gamer = gamerOptional.get();
            Optional<Boolean> staffChatEnabledOptional = gamer.getProperty(GamerProperty.STAFF_CHAT);
            if(staffChatEnabledOptional.isPresent()){
                staffChatEnabled = !staffChatEnabledOptional.get();
            }

            gamer.saveProperty(GamerProperty.STAFF_CHAT, staffChatEnabled);
            gamer.saveProperty(GamerProperty.ALLY_CHAT, false);
            gamer.saveProperty(GamerProperty.CLAN_CHAT, false);

            Component result = Component.text((staffChatEnabled ? "enabled" : "disabled"), (staffChatEnabled ? NamedTextColor.GREEN : NamedTextColor.RED));
            UtilMessage.simpleMessage(player, "Command", Component.text("Staff Chat: ").append(result));
        }
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.HELPER;
    }


}
