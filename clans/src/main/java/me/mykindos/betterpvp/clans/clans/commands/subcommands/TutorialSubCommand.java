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
import me.mykindos.betterpvp.core.components.champions.Role;
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
            UtilMessage.deserialize("Enjoy the game how you want to play it, there is no right way to play Clans. If you like building, combat, farming, bosses, and more, there is something for you. Play with up to <green>7</green> friends and see where your journey takes you."),
            UtilMessage.deserialize("The core of Clans is the <gold>Clan</gold>. You can make one by running ")
                    .append(UtilMessage.deserialize("<yellow>/c create <name></yellow>").clickEvent(ClickEvent.suggestCommand("/c create ")))
                    .append(UtilMessage.deserialize(", invite friends by running "))
                    .append(UtilMessage.deserialize("<yellow>/c invite <player></yellow>").clickEvent(ClickEvent.suggestCommand("/c invite ")))
                    .append(UtilMessage.deserialize(", and join a <yellow>Clan</yellow> you are invited to by running "))
                    .append(UtilMessage.deserialize("<yellow>/c join <clan>").clickEvent(ClickEvent.suggestCommand("/c join "))),
            UtilMessage.deserialize("<gold>Clans</gold> can claim territory by running ")
                    .append(UtilMessage.deserialize("<yellow>/c claim</yellow>").clickEvent(ClickEvent.runCommand("/c help claim")))
                    .append(UtilMessage.deserialize(", which you can use to store items. <red>Enemy</red> <gold>Clans</gold> can use cannons to besiege you, after reaching <green>100</green> <red>dominance</red> on you.")).appendNewline()
                    .append(UtilMessage.deserialize(" <i>Related commands:</i> "))
                    .append(UtilMessage.deserialize("<yellow>/c unclaim</yellow> ").clickEvent(ClickEvent.runCommand("/c help unclaim")))
                    .append(UtilMessage.deserialize("<yellow>/c enemy <clan></yellow> ").clickEvent(ClickEvent.runCommand("/c help enemy")))
                    .append(UtilMessage.deserialize("<yellow>/c neutral <clan></yellow> ").clickEvent(ClickEvent.runCommand("/c help neutral"))),
            UtilMessage.deserialize("In your territory, you can set a location by running ")
                    .append(UtilMessage.deserialize("<yellow>/c sethome</yellow> ").clickEvent(ClickEvent.runCommand("/c help sethome")))
                    .append(UtilMessage.deserialize("that you can teleport back to by running "))
                    .append(UtilMessage.deserialize("<yellow>/c home</yellow>").clickEvent(ClickEvent.runCommand("/c help home")))
                    .append(UtilMessage.deserialize(". You can <green><bold>ally</bold></green> with other <gold>Clans</gold> by running "))
                    .append(UtilMessage.deserialize("<yellow>/c ally <clan></yellow>").clickEvent(ClickEvent.suggestCommand("/c ally ")))
                    .append(UtilMessage.deserialize(" and optionally trust them by running "))
                    .append(UtilMessage.deserialize("<yellow>/c trust <clan></yellow> ").clickEvent(ClickEvent.runCommand("/c help trust"))),
            UtilMessage.deserialize("You may only have up to <green>8</green> players between <green><bold>Allied</bold></green> and <aqua><bold>Own</bold></aqua> <gold>Clans</gold>. See ")
                    .append(UtilMessage.deserialize("<yellow>/c help</yellow>").clickEvent(ClickEvent.runCommand("/c help")))
                    .append(UtilMessage.deserialize(" for more information on Clan commands.")),
            UtilMessage.deserialize("There are currently <green>6</green> <gold>Champions</gold> classes, one for each armor set. Each has its strengths and weaknesses. There are many different skills to choose between, pick and choose the ones that fit your playstyle most"),
            Component.text(Role.ASSASSIN.getName(), Role.ASSASSIN.getColor()).appendNewline()
                    .append(UtilMessage.deserialize("Assassin is a quick and agile class. With relatively low health and strong counters, this is not an easy class to be caught out in. Primarily suited for attacking distracted or otherwise occupied enemies, it will struggle against enemies that are prepared.")),
            Component.text(Role.KNIGHT.getName(), Role.KNIGHT.getColor()).appendNewline()
                    .append(UtilMessage.deserialize("Knight is a strong, aggressive class. It has thrives being on the attack, having options to increase damage, while also having a few that can keep them alive long enough to kill their opponent.")),
            Component.text(Role.BRUTE.getName(), Role.BRUTE.getColor()).appendNewline()
                    .append(UtilMessage.deserialize("Brute is a powerhouse that is rarely moved by others. It has good crowd control and defensive abilities.")),
            Component.text(Role.RANGER.getName(), Role.RANGER.getColor()).appendNewline()
                    .append(UtilMessage.deserialize("Ranger is a ranged class skilled in the use of the bow. There are different options, rewarding precision, patience, or long distance accuracy.")),
            Component.text(Role.MAGE.getName(), Role.MAGE.getColor()).appendNewline()
                    .append(UtilMessage.deserialize("Mage is a class skilled with different elements. Choose between life, ice, fire, and earth to support your teammates, trap the enemy, and kill them.")),
            Component.text(Role.WARLOCK.getName(), Role.WARLOCK.getColor()).appendNewline()
                    .append(UtilMessage.deserialize("Warlock is a class focused on health. Some abilities require the sacrifice in health, others punish enemies for proximity and low health.")),
            UtilMessage.deserialize("There is more than just combat, you could also level up other areas. Level up your Progression in <blue>Fishing</blue>, <silver>Mining</silver>, and <brown>Woodcutting</brown>")


            //Kits
            //Progression
            //
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
                runTutorial(player, iterator, 3 * 20);
            }
        });
    }

    private void runTutorial(Player player, Iterator<Component> iterator, long delay) {
        UtilMessage.message(player, "Tutorial", iterator.next());
        UtilServer.runTaskLater(clans, true, () -> {
            if (iterator.hasNext()) {
                runTutorial(player, iterator, 3 * 20);
            }
        }, delay);
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
