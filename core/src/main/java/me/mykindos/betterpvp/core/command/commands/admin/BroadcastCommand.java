package me.mykindos.betterpvp.core.command.commands.admin;

import com.comphenix.protocol.wrappers.MinecraftKey;
import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.WorldHandler;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.packs.DataPack;

import java.util.Optional;

public class BroadcastCommand extends Command {
    private final GamerManager gamerManager;

    @Inject
    public BroadcastCommand(GamerManager gamerManager){
        this.gamerManager = gamerManager;

        aliases.add("bc");
    }

    @Override
    public String getName() {
        return "broadcast";
    }

    @Override
    public String getDescription() {
        return "Send an emphasized message to the server";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        if(args.length == 0) {
            UtilMessage.message(player, "Core", "You must specify a message");
            return;
        }

        Component message = Component.empty().append(client.getRank().getPlayerNameMouseOver(player.getName()).decorate(TextDecoration.BOLD))
                        .append(UtilMessage.deserialize(" <red>%s", String.join(" ", args)));

        Sound notification = Sound.sound(Key.key(Key.MINECRAFT_NAMESPACE, "block.note_block.bell"), Sound.Source.NEUTRAL, 1.0f, 1.0f);
        for (Player playerToSend : Bukkit.getOnlinePlayers()) {
            UtilMessage.message(playerToSend, message);
            playerToSend.playSound(notification, Sound.Emitter.self());
        }
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.HELPER;
    }

}
