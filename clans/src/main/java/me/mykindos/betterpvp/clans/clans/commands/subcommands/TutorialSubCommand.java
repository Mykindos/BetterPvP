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
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Singleton
@SubCommand(ClanCommand.class)
public class TutorialSubCommand extends ClanSubCommand {

    private final Clans clans;

    List<Component> tutorialText = new ArrayList<>(List.of(
            UtilMessage.deserialize("Welcome to <gold>Clans</gold>! <gold>Clans</gold> is a long term factions like gamemode, with <light_purple>special weapons</light_purple>, <dark_purple>classes</dark_purple>, and <dark_aqua>bosses</dark_aqua>."),
            UtilMessage.deserialize("<gold>Clans</gold> can be a difficult game, it is possible to lose everything and start from scratch. This is not common, but can happen. <dark_green>Seasons</dark_green> periodically reset, and everyone starts from scratch."),
            UtilMessage.deserialize("Enjoy the game how you want to play it, there is no right way to play <gold>Clans</gold>. If you like <white>building</white>, <dark_red>combat</dark_red>, <dark_green>farming</dark_green>, <dark_aqua>bosses</dark_aqua>, and more, there is something for you. Play with up to <green>7</green> friends and see where your journey takes you."),
            UtilMessage.deserialize("The core of <gold>Clans</gold> is the <aqua>Clan</aqua>. You can make one by running ")
                    .append(UtilMessage.deserialize("<yellow>/c create <name></yellow>").clickEvent(ClickEvent.suggestCommand("/c create ")))
                    .append(UtilMessage.deserialize(", invite friends by running "))
                    .append(UtilMessage.deserialize("<yellow>/c invite <player></yellow>").clickEvent(ClickEvent.suggestCommand("/c invite ")))
                    .append(UtilMessage.deserialize(", and join a <aqua>Clan</aqua> you are invited to by running "))
                    .append(UtilMessage.deserialize("<yellow>/c join <clan>").clickEvent(ClickEvent.suggestCommand("/c join "))),
            UtilMessage.deserialize("<aqua>Clans</aqua> can claim territory by running ")
                    .append(UtilMessage.deserialize("<yellow>/c claim</yellow>").clickEvent(ClickEvent.runCommand("/c help claim")))
                    .append(UtilMessage.deserialize(", which you can use to store items. <red>Enemy</red> <aqua>Clans</aqua> can use cannons to besiege you, after reaching <green>100</green> <red>dominance</red> on you.")).appendNewline()
                    .append(UtilMessage.deserialize(" <i>Related commands:</i> "))
                    .append(UtilMessage.deserialize("<yellow>/c unclaim</yellow> ").clickEvent(ClickEvent.runCommand("/c help unclaim")))
                    .append(UtilMessage.deserialize("<yellow>/c enemy <clan></yellow> ").clickEvent(ClickEvent.runCommand("/c help enemy")))
                    .append(UtilMessage.deserialize("<yellow>/c neutral <clan></yellow> ").clickEvent(ClickEvent.runCommand("/c help neutral"))),
            UtilMessage.deserialize("In your territory, you can set a location by running ")
                    .append(UtilMessage.deserialize("<yellow>/c sethome</yellow> ").clickEvent(ClickEvent.runCommand("/c help sethome")))
                    .append(UtilMessage.deserialize("that you can teleport back to by running "))
                    .append(UtilMessage.deserialize("<yellow>/c home</yellow>").clickEvent(ClickEvent.runCommand("/c help home")))
                    .append(UtilMessage.deserialize(". You can <green><bold>ally</bold></green> with other <aqua>Clans</aqua> by running "))
                    .append(UtilMessage.deserialize("<yellow>/c ally <clan></yellow>").clickEvent(ClickEvent.suggestCommand("/c ally ")))
                    .append(UtilMessage.deserialize(" and optionally trust them by running "))
                    .append(UtilMessage.deserialize("<yellow>/c trust <clan></yellow> ").clickEvent(ClickEvent.runCommand("/c help trust"))),
            UtilMessage.deserialize("You may only have up to <green>8</green> players between <green><bold>Allied</bold></green> and <aqua><bold>Own</bold></aqua> <aqua>Clans</aqua>. See ")
                    .append(UtilMessage.deserialize("<yellow>/c help</yellow>").clickEvent(ClickEvent.runCommand("/c help")))
                    .append(UtilMessage.deserialize(" for more information on Clan commands.")),
            UtilMessage.deserialize("There are currently <green>6</green> <gold>Champions</gold> <dark_purple>classes</dark_purple>, one for each armor set. Each has its strengths and weaknesses. There are many different skills to choose between, pick and choose the ones that fit your playstyle most"),
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
            UtilMessage.deserialize("There is more than just combat, you could also level up other areas. Level up your Progression in <blue>Fishing</blue>, <white>Mining</white>, and <dark_red>Woodcutting</dark_red>"),
            Component.text("Fishing", NamedTextColor.BLUE).appendNewline()
                    .append(UtilMessage.deserialize("Take your rod and cast it into the <blue>Lake</blue>, fishing around for fish and treasures. Level up with catches, cast baits to entice fish, and upgrade your rod to catch heavier fish. Take your catch and sell it at shops, or take your treasure home.")),
            Component.text("Mining", NamedTextColor.WHITE).appendNewline()
                    .append(UtilMessage.deserialize("Take your pick and make your way to the mines, for there are riches in store. Earn experience, with 5x more in <white>Fields</white>. Higher levels lead to more drops and faster mining speeds.")),
            UtilMessage.deserialize("In each cardinal direction there is a <green>Safezone</green> that contains <white>Shops</white>. Here, you can sell most items and buy essential goods."),
            UtilMessage.deserialize("In the center of the map is <white>Fields</white>. A high traffic area, here you can find ores, places to fish, and a teleporter to events. Use ")
                    .append(UtilMessage.deserialize("<yellow>/map</yellow>").clickEvent(ClickEvent.runCommand("/map")))
                    .append(UtilMessage.deserialize(" to receive a map of the server.")),
            UtilMessage.deserialize("From boss events, you have a chance to receive <light_purple><bold>Legendary Weapons</bold></light_purple>. These are strong, special weapons, imbued with unique capabilities."),
            UtilMessage.deserialize("Good luck in your adventures and remember to have fun.")
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
