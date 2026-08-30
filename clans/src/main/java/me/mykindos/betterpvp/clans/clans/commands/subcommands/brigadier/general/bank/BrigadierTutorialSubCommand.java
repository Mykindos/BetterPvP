package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general.bank;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierTutorialSubCommand extends ClanBrigadierCommand {

    //no yellow or white, it is not easy to read in a book
    private static final List<Component> tutorialText = new ArrayList<>(List.of(
            UtilMessage.deserialize("<black>Welcome to <gold>Clans</gold>! <gold>Clans</gold> is a long term factions like gamemode, with <light_purple>special weapons</light_purple>, <dark_purple>classes</dark_purple>, and <dark_aqua>bosses</dark_aqua>")
                    .appendNewline()
                    .appendNewline()
                    .append(UtilMessage.deserialize("<black>You can open this book again at any time by running "))
                    .append(UtilMessage.copyCommand("/c tutorial")),
            UtilMessage.deserialize("<black><b>Table of contents</b>").appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Overview", 4)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Clan", 6)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Territory", 7)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Alliances", 8)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Map", 10)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Shops", 11)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Fields", 12)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Events/Bosses", 13)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Classes/Kits", 14)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Assassin", 15)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Knight", 16)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Brute", 17)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Ranger", 18)).appendNewline()

            ,
            UtilMessage.deserialize("<black><b>Table of contents</b>").appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Mage", 19)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Warlock", 20)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Custom Items", 21)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Reinforced", 23)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Upgraded Weapons", 24)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Progression", 25)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Fishing", 26)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Mining", 27)).appendNewline()
                    .append(UtilMessage.tableOfContentsEntry("Woodcutting", 28)).appendNewline()
            ,
            /*4*/ UtilMessage.deserialize("<black><gold>Clans</gold> can be a difficult game, it is possible to lose everything and start from scratch. This is not common, but can happen. <dark_green>Seasons</dark_green> periodically reset, and everyone starts from scratch."),
            /*5*/ UtilMessage.deserialize("<black>Enjoy the game how you want to play, there is no right way to play <gold>Clans</gold>. If you like <blue>building</blue>, <dark_red>combat</dark_red>, <dark_green>farming</dark_green>, <dark_aqua>bosses</dark_aqua>, and more, there is something for you.\nPlay with up to <green>7</green> friends and see where your journey takes you."),
            /*6*/ UtilMessage.deserialize("<black>The core of <gold>Clans</gold> is the <aqua>Clan</aqua>. You can make one by running ")
                    .append(UtilMessage.copyCommand("/c create <name>", "/c create "))
                    .append(UtilMessage.deserialize("<black>, invite friends by running "))
                    .append(UtilMessage.copyCommand("/c invite <player>", "/c invite ")
                            .append(UtilMessage.deserialize("<black>, and join a <aqua>Clan</aqua> you are invited to by running "))
                            .append(UtilMessage.copyCommand("/c join <clan>", "/c join "))),
            /*7*/ UtilMessage.deserialize("<black><aqua>Clans</aqua> can claim territory by running ")
                    .append(UtilMessage.copyCommand("/c claim", "/c help claim"))
                    .append(UtilMessage.deserialize("<black>, which you can use to store items. <red>Enemy</red> <aqua>Clans</aqua> can use cannons to besiege you, after reaching <green>100</green> <red>dominance</red> on you."))
                    .appendNewline()
                    .append(UtilMessage.deserialize("<black><i>Related commands:</i>"))
                    .appendNewline()
                    .append(UtilMessage.copyCommand("/c unclaim", "/c help unclaim"))
                    .appendNewline()
                    .append(UtilMessage.copyCommand("/c enemy <clan>", "/c help enemy"))
                    .appendNewline()
                    .append(UtilMessage.copyCommand("/c neutral <clan>", "/c help neutral")),
            /*8*/ UtilMessage.deserialize("<black>In your territory, you can set a location by running ")
                    .append(UtilMessage.copyCommand("/c setcore", "/c help setcore"))
                    .append(UtilMessage.deserialize("<black> that you can teleport back to by running "))
                    .append(UtilMessage.copyCommand("/c core", "/c help home"))
                    .append(UtilMessage.deserialize("<black>. You can <green><bold>ally</bold></green> with other <aqua>Clans</aqua> by running "))
                    .append(UtilMessage.copyCommand("/c ally <clan>", "/c ally "))
                    .append(UtilMessage.deserialize("<black> and optionally trust them by running "))
                    .append(UtilMessage.copyCommand("/c trust <clan>", "/c help trust")),
            /*9*/ UtilMessage.deserialize("<black>You may only have up to <green>8</green> players between <green><bold>Allied</bold></green> and <aqua><bold>Own</bold></aqua> <aqua>Clans</aqua>. See ")
                    .append(UtilMessage.copyCommand("/c help", "/c help"))
                    .append(UtilMessage.deserialize("<black> for more information on Clan commands.")),
            /*10*/ UtilMessage.deserialize("<black>There are a few different areas to explore in the world. Use ")
                    .append(UtilMessage.copyCommand("/map"))
                    .append(UtilMessage.deserialize("<black> to see the map and find your way around. Maps show your clanmates. During a pillage, they also show enemies.")),
            /*11*/ UtilMessage.deserialize("<black>In each cardinal direction there is a <green>Safezone</green> that contains <gray>Shops</gray>. Here, you can sell most items, buy essential goods, access dungeons, and change your class. " +
                    "\nYou can teleport here from the world spawn."),
            /*12*/ UtilMessage.deserialize("<black>In the center of the map is <gray>Fields</gray>. A high traffic area, here you can find ores, places to fish, and a teleporter to events."),
            /*13*/ UtilMessage.deserialize("<black>Periodically events will occur, which you can access by using the portal in <gray>Fields</gray>. These events can drop <light_purple><bold>Legendary Weapons</bold></light_purple>. These are strong, special weapons, imbued with unique capabilities. You can also find runes and dungeon tokens here."),
            /*14*/ UtilMessage.deserialize("<black>There are currently <green>6</green> <gold>Champions</gold> <dark_purple>classes</dark_purple>, one for each armor set. Each has its strengths and weaknesses. There are many different <gray>skills</gray> to choose between, pick and choose the ones that fit your playstyle most. You can adjust <gray>skills</gray> for any <dark_purple>class</dark_purple> at an enchanting table."),
            /*15*/ Component.text(Role.ASSASSIN.getName(), Role.ASSASSIN.getColor()).appendNewline()
                    .append(UtilMessage.deserialize(Role.ASSASSIN.getDescription()).color(NamedTextColor.BLACK)),
            /*16*/ Component.text(Role.KNIGHT.getName(), Role.KNIGHT.getColor()).appendNewline()
                    .append(UtilMessage.deserialize(Role.KNIGHT.getDescription()).color(NamedTextColor.BLACK)),
            /*17*/ Component.text(Role.BRUTE.getName(), Role.BRUTE.getColor()).appendNewline()
                    .append(UtilMessage.deserialize(Role.BRUTE.getDescription()).color(NamedTextColor.BLACK)),
            /*18*/ Component.text(Role.RANGER.getName(), Role.RANGER.getColor()).appendNewline()
                    .append(UtilMessage.deserialize(Role.RANGER.getDescription()).color(NamedTextColor.BLACK)),
            /*19*/ Component.text(Role.MAGE.getName(), Role.MAGE.getColor()).appendNewline()
                    .append(UtilMessage.deserialize(Role.MAGE.getDescription()).color(NamedTextColor.BLACK)),
            /*20*/ Component.text(Role.WARLOCK.getName(), Role.WARLOCK.getColor()).appendNewline()
                    .append(UtilMessage.deserialize(Role.WARLOCK.getDescription()).color(NamedTextColor.BLACK)),
            /*21*/ UtilMessage.deserialize("<black>There are custom items that you can find, buy, or craft. You can see the craftable items and their recipes by running ")
                    .append(UtilMessage.copyCommand("/c recipes"))
                    .append(UtilMessage.deserialize("<black>. These items can be used to give you an edge in combat, or to help you gather resources.")),
            /*22*/ UtilMessage.deserialize("<black>Some of these items are only available from events, dungeons, or raids, so keep an eye out for those."),
            /*23*/ UtilMessage.deserialize("<black>Classes/Kits are identified by armor, you automatically have a class. You can craft the specific armor set to get the reinforced set. You may only wear the reinforced set of your class. Reinforced pieces give increased health."),
            /*24*/ UtilMessage.deserialize("<black>Your basic weapons can be upgraded. <gold>Booster</gold> (made with gold blocks) or <aqua>Power</aqua> (made with diamond blocks). <gold>Booster</gold> increase the relevant <gray>skill</gray> by <gold>+1</gold> and <aqua>Power</aqua> increases damage by <gold>+0.5</gold>. <dark_purple>Ancient</dark_purple> weapons (found from events) combine the effects of both!"),
            /*25*/ UtilMessage.deserialize("<black>There is more than just combat, you could also level up other areas. Level up your Progression in <blue>Fishing</blue>, <gray>Mining</gray>, and <dark_red>Woodcutting</dark_red>"),
            /*26*/ Component.text("Fishing", NamedTextColor.BLUE).appendNewline()
                    .append(UtilMessage.deserialize("<black>Take your rod and cast it into the <blue>Lake</blue>, fishing around for fish and treasures. Level up with catches, cast baits to entice fish, and upgrade your rod to catch heavier fish. Take your catch and sell it at shops, or take your treasure home.")),
            /*27*/ Component.text("Mining", NamedTextColor.GRAY).appendNewline()
                    .append(UtilMessage.deserialize("<black>Take your pick and make your way to the mines, for there are riches in store. Earn experience, with 5x more in <gray>Fields</gray>. Higher levels lead to more drops and faster mining speeds.")),
            /*28*/ Component.text("Woodcutting", NamedTextColor.DARK_RED).appendNewline()
                    .append(UtilMessage.deserialize("<black>Take your axe and find a tree to chop. Level up with each log you chop, giving you access to new perks and abilities.")),
            /*29*/ UtilMessage.deserialize("<black>Good luck in your adventures and remember to have fun.")
    ));

    public static final Book book = Book.book(Component.text("Clans Tutorial", NamedTextColor.GOLD), Component.text("BetterPvP", NamedTextColor.GOLD), tutorialText);

    @Inject
    protected BrigadierTutorialSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "tutorial";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Open the clans tutorial book";
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     *
     * @return the builder to be used in Build
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return IBrigadierCommand.literal(getName())
                .executes(context -> {
                    final Player executor = getPlayerFromExecutor(context);
                    executor.openBook(book);
                    return Command.SINGLE_SUCCESS;
                });
    }

}
