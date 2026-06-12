package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Singleton
@SubCommand(ClanCommand.class)
public class TutorialSubCommand extends ClanSubCommand {

    private static final NamedTextColor LIGHT_PURPLE = NamedTextColor.LIGHT_PURPLE;
    private static final NamedTextColor DARK_PURPLE = NamedTextColor.DARK_PURPLE;
    private static final NamedTextColor DARK_AQUA = NamedTextColor.DARK_AQUA;
    private static final NamedTextColor DARK_GREEN = NamedTextColor.DARK_GREEN;
    private static final NamedTextColor DARK_RED = NamedTextColor.DARK_RED;
    private static final NamedTextColor BLUE = NamedTextColor.BLUE;

    private static Component clans() {
        return Component.text("Clans", NamedTextColor.GOLD);
    }

    private static Component bold(Component component) {
        return component.decoration(TextDecoration.BOLD, true);
    }

    //no yellow or white, it is not easy to read in a book
    private static final List<Component> tutorialText = new ArrayList<>(List.of(
            Translations.component("clans.tutorial.intro",
                            clans(), clans(),
                            Component.text("special weapons", LIGHT_PURPLE),
                            Component.text("classes", DARK_PURPLE),
                            Component.text("bosses", DARK_AQUA))
                    .color(NamedTextColor.BLACK)
                    .appendNewline()
                    .appendNewline()
                    .append(Translations.component("clans.tutorial.reopen").color(NamedTextColor.BLACK).append(Component.text(" ")))
                    .append(UtilMessage.copyCommand("/c tutorial")),
            bold(Translations.component("clans.tutorial.toc-title")).color(NamedTextColor.BLACK).appendNewline()
                    // SKIPPED: tableOfContentsEntry takes a plain String embedded into a clickable
                    // changePage() component; cannot accept a translatable key. Entry labels left literal.
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
            bold(Translations.component("clans.tutorial.toc-title")).color(NamedTextColor.BLACK).appendNewline()
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
            /*4*/ Translations.component("clans.tutorial.difficulty",
                            clans(),
                            Component.text("Seasons", DARK_GREEN))
                    .color(NamedTextColor.BLACK),
            /*5*/ Translations.component("clans.tutorial.enjoy",
                            clans(),
                            Component.text("building", BLUE),
                            Component.text("combat", DARK_RED),
                            Component.text("farming", DARK_GREEN),
                            Component.text("bosses", DARK_AQUA),
                            Component.text("7", NamedTextColor.GREEN))
                    .color(NamedTextColor.BLACK),
            /*6*/ Translations.component("clans.tutorial.clan-core",
                            clans(),
                            Component.text("Clan", NamedTextColor.AQUA))
                    .color(NamedTextColor.BLACK).append(Component.text(" "))
                    .append(UtilMessage.copyCommand("/c create <name>", "/c create "))
                    .append(Translations.component("clans.tutorial.clan-invite").color(NamedTextColor.BLACK).append(Component.text(" ")))
                    .append(UtilMessage.copyCommand("/c invite <player>", "/c invite ")
                    .append(Translations.component("clans.tutorial.clan-join",
                                    Component.text("Clan", NamedTextColor.AQUA))
                            .color(NamedTextColor.BLACK).append(Component.text(" ")))
                    .append(UtilMessage.copyCommand("/c join <clan>", "/c join "))),
            /*7*/ Translations.component("clans.tutorial.territory-claim",
                            Component.text("Clans", NamedTextColor.AQUA))
                    .color(NamedTextColor.BLACK).append(Component.text(" "))
                    .append(UtilMessage.copyCommand("/c claim", "/c help claim"))
                    .append(Translations.component("clans.tutorial.territory-info",
                                    Component.text("Enemy", NamedTextColor.RED),
                                    Component.text("Clans", NamedTextColor.AQUA),
                                    Component.text("100", NamedTextColor.GREEN),
                                    Component.text("dominance", NamedTextColor.RED))
                            .color(NamedTextColor.BLACK))
                    .appendNewline()
                    .append(Translations.component("clans.tutorial.related-commands").color(NamedTextColor.BLACK).decoration(TextDecoration.ITALIC, true))
                    .appendNewline()
                    .append(UtilMessage.copyCommand("/c unclaim", "/c help unclaim"))
                    .appendNewline()
                    .append(UtilMessage.copyCommand("/c enemy <clan>", "/c help enemy"))
                    .appendNewline()
                    .append(UtilMessage.copyCommand("/c neutral <clan>", "/c help neutral")),
            /*8*/ Translations.component("clans.tutorial.core-set").color(NamedTextColor.BLACK).append(Component.text(" "))
                    .append(UtilMessage.copyCommand("/c setcore", "/c help setcore"))
                    .append(Translations.component("clans.tutorial.core-teleport").color(NamedTextColor.BLACK))
                    .append(UtilMessage.copyCommand("/c core", "/c help home"))
                    .append(Translations.component("clans.tutorial.core-ally",
                                    bold(Component.text("ally", NamedTextColor.GREEN)),
                                    Component.text("Clans", NamedTextColor.AQUA))
                            .color(NamedTextColor.BLACK).append(Component.text(" ")))
                    .append(UtilMessage.copyCommand("/c ally <clan>", "/c ally "))
                    .append(Translations.component("clans.tutorial.core-trust").color(NamedTextColor.BLACK))
                    .append(UtilMessage.copyCommand("/c trust <clan>", "/c help trust")),
            /*9*/ Translations.component("clans.tutorial.alliance-limit",
                            Component.text("8", NamedTextColor.GREEN),
                            bold(Component.text("Allied", NamedTextColor.GREEN)),
                            bold(Component.text("Own", NamedTextColor.AQUA)),
                            Component.text("Clans", NamedTextColor.AQUA))
                    .color(NamedTextColor.BLACK).append(Component.text(" "))
                    .append(UtilMessage.copyCommand("/c help", "/c help"))
                    .append(Translations.component("clans.tutorial.alliance-help").color(NamedTextColor.BLACK)),
            /*10*/ Translations.component("clans.tutorial.map-intro").color(NamedTextColor.BLACK).append(Component.text(" "))
                    .append(UtilMessage.copyCommand("/map"))
                    .append(Translations.component("clans.tutorial.map-info").color(NamedTextColor.BLACK)),
            /*11*/ Translations.component("clans.tutorial.shops",
                            Component.text("Safezone", NamedTextColor.GREEN),
                            Component.text("Shops", NamedTextColor.GRAY))
                    .color(NamedTextColor.BLACK),
            /*12*/ Translations.component("clans.tutorial.fields",
                            Component.text("Fields", NamedTextColor.GRAY))
                    .color(NamedTextColor.BLACK),
            /*13*/ Translations.component("clans.tutorial.events",
                            Component.text("Fields", NamedTextColor.GRAY),
                            bold(Component.text("Legendary Weapons", LIGHT_PURPLE)))
                    .color(NamedTextColor.BLACK),
            /*14*/ Translations.component("clans.tutorial.classes",
                            Component.text("6", NamedTextColor.GREEN),
                            Component.text("Champions", NamedTextColor.GOLD),
                            Component.text("classes", DARK_PURPLE),
                            Component.text("skills", NamedTextColor.GRAY),
                            Component.text("skills", NamedTextColor.GRAY),
                            Component.text("class", DARK_PURPLE))
                    .color(NamedTextColor.BLACK),
            /*15*/ Role.ASSASSIN.getDisplayName().color(Role.ASSASSIN.getColor()).appendNewline()
                    .append(Role.ASSASSIN.getDescriptionComponent().color(NamedTextColor.BLACK)),
            /*16*/ Role.KNIGHT.getDisplayName().color(Role.KNIGHT.getColor()).appendNewline()
                    .append(Role.KNIGHT.getDescriptionComponent().color(NamedTextColor.BLACK)),
            /*17*/ Role.BRUTE.getDisplayName().color(Role.BRUTE.getColor()).appendNewline()
                    .append(Role.BRUTE.getDescriptionComponent().color(NamedTextColor.BLACK)),
            /*18*/ Role.RANGER.getDisplayName().color(Role.RANGER.getColor()).appendNewline()
                    .append(Role.RANGER.getDescriptionComponent().color(NamedTextColor.BLACK)),
            /*19*/ Role.MAGE.getDisplayName().color(Role.MAGE.getColor()).appendNewline()
                    .append(Role.MAGE.getDescriptionComponent().color(NamedTextColor.BLACK)),
            /*20*/ Role.WARLOCK.getDisplayName().color(Role.WARLOCK.getColor()).appendNewline()
                    .append(Role.WARLOCK.getDescriptionComponent().color(NamedTextColor.BLACK)),
            /*21*/ Translations.component("clans.tutorial.custom-items").color(NamedTextColor.BLACK).append(Component.text(" "))
                    .append(UtilMessage.copyCommand("/c recipes"))
                    .append(Translations.component("clans.tutorial.custom-items-info").color(NamedTextColor.BLACK)),
            /*22*/ Translations.component("clans.tutorial.custom-items-rare").color(NamedTextColor.BLACK),
            /*23*/ Translations.component("clans.tutorial.reinforced").color(NamedTextColor.BLACK),
            /*24*/ Translations.component("clans.tutorial.upgraded-weapons",
                            Component.text("Booster", NamedTextColor.GOLD),
                            Component.text("Power", NamedTextColor.AQUA),
                            Component.text("Booster", NamedTextColor.GOLD),
                            Component.text("skill", NamedTextColor.GRAY),
                            Component.text("+1", NamedTextColor.GOLD),
                            Component.text("Power", NamedTextColor.AQUA),
                            Component.text("+0.5", NamedTextColor.GOLD),
                            Component.text("Ancient", DARK_PURPLE))
                    .color(NamedTextColor.BLACK),
            /*25*/ Translations.component("clans.tutorial.progression",
                            Component.text("Fishing", BLUE),
                            Component.text("Mining", NamedTextColor.GRAY),
                            Component.text("Woodcutting", DARK_RED))
                    .color(NamedTextColor.BLACK),
            /*26*/ Translations.component("clans.tutorial.fishing-title").color(BLUE).appendNewline()
                    .append(Translations.component("clans.tutorial.fishing",
                                    Component.text("Lake", BLUE))
                            .color(NamedTextColor.BLACK)),
            /*27*/ Translations.component("clans.tutorial.mining-title").color(NamedTextColor.GRAY).appendNewline()
                    .append(Translations.component("clans.tutorial.mining",
                                    Component.text("Fields", NamedTextColor.GRAY))
                            .color(NamedTextColor.BLACK)),
            /*28*/ Translations.component("clans.tutorial.woodcutting-title").color(DARK_RED).appendNewline()
                    .append(Translations.component("clans.tutorial.woodcutting").color(NamedTextColor.BLACK)),
            /*29*/ Translations.component("clans.tutorial.good-luck").color(NamedTextColor.BLACK)
    ));

    public static final Book book = Book.book(Component.text("Clans Tutorial", NamedTextColor.GOLD), Component.text("BetterPvP", NamedTextColor.GOLD), tutorialText);

    /**
     * Builds a copy of the tutorial book with every (translatable) page resolved into the viewer's locale.
     * <p>
     * The static {@link #book} is a language-neutral template; book content is NOT run through the
     * per-viewer {@code GlobalTranslator} on send (unlike chat/action bars), so opening it directly shows
     * raw translation keys. Resolve server-side here for the recipient instead.
     */
    public static Book localizedBook(Player player) {
        final Locale locale = player.locale();
        final List<Component> pages = new ArrayList<>(tutorialText.size());
        for (Component page : tutorialText) {
            pages.add(Translations.render(page, locale));
        }
        return Book.book(Translations.render(book.title(), locale), Translations.render(book.author(), locale), pages);
    }

    @Inject
    public TutorialSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "tutorial";
    }

    @Override
    public String getDescription() {
        return "clans.command.tutorial.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        player.openBook(localizedBook(player));
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
