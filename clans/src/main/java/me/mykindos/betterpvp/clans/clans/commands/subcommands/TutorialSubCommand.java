package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Singleton
@SubCommand(ClanCommand.class)
public class TutorialSubCommand extends ClanSubCommand {

    private final Clans clans;

    List<Component> tutorialText = new ArrayList<>(List.of(
            UtilMessage.deserialize("Welcome to Clans! Clans is a long term factions like gamemode, with special weapons, kits, and bosses."),
            UtilMessage.deserialize("Clans can be a difficult game, it is possible to lose everything and start from scratch. This is not common, but can happen. Seasons periodically reset, and everyone starts from scratch."),
            UtilMessage.deserialize("Enjoy the game how you want to play it, there is no right way to play Clans. If you like building, combat, farming, bosses, and more, there is something for you. Play with up to 7 friends and see where your journey takes you."),
            UtilMessage.deserialize("The core of Clans is the <yellow>Clan</yellow>. You can make one by running")
                    .append(UtilMessage.deserialize("<yellow>/c create <name></yellow>").clickEvent(ClickEvent.suggestCommand("/c create ")))
                    .append(UtilMessage.deserialize(", invite friends by running")), //and then join
            UtilMessage.deserialize("")
            //Clans
            //Claiming
            //Sieges
    ));

    @Inject
    public TutorialSubCommand(ClanManager clanManager, ClientManager clientManager, Clans clans) {
        super(clanManager, clientManager);

        this.clans = clans;
    }

    @Override
    public String getName() {
        return "tutorial";
    }

    @Override
    public String getDescription() {
        return "Runs a tutorial for clans";
    }

    @Override
    public void execute(Player player, Client client, String... args) {;
        UtilServer.runTaskAsync(clans, () -> {
            Iterator<Component> iterator = tutorialText.iterator();
            if (iterator.hasNext()) {
                runTutorial(player, iterator, 1 * 20);
            }
        });
    }

    private void runTutorial(Player player, Iterator<Component> iterator, long delay) {
        UtilMessage.message(player, "Tutorial", iterator.next());
        UtilServer.runTaskLater(clans, true, () -> {
            if (iterator.hasNext()) {
                runTutorial(player, iterator, 1 * 20);
            }
        }, delay);
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
