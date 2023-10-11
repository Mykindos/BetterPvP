package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class InfoSubCommand extends ClanSubCommand {

    @Inject
    public InfoSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "View another clans information";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <clan>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, "Clans", "You must specify a clan name.");
            return;
        }

        String clanName = String.join(" ", args).trim();
        Optional<Clan> clanOptional = clanManager.getClanByName(clanName);
        if (clanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "Could not find a clan with that name.");
            return;
        }

        Clan target = clanOptional.get();


        Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);

        MenuManager.openMenu(player, new ClanMenu(player, playerClan, target));

        //ClanRelation clanRelation = clanManager.getRelation(playerClan, target);
        //Component component = Component.text(target.getName() + " Information: ", clanRelation.getPrimary()).appendNewline()
        //        .append(Component.text("Age: ", NamedTextColor.WHITE)).append(Component.text(target.getAge(), NamedTextColor.YELLOW)).appendNewline()
        //        .append(Component.text("Territory: ", NamedTextColor.WHITE)).append(Component.text(target.getTerritory().size() + "/" + (3 + target.getMembers().size()), NamedTextColor.YELLOW)).appendNewline()
        //        .append(Component.text("Allies: ", NamedTextColor.WHITE)).append(UtilMessage.deserialize(clanManager.getAllianceList(player, target))).appendNewline()
        //        .append(Component.text("Enemies: ", NamedTextColor.WHITE)).append(UtilMessage.deserialize(clanManager.getEnemyListDom(player, target))).appendNewline()
        //        .append(Component.text("Members: ", NamedTextColor.WHITE)).append(UtilMessage.deserialize(clanManager.getMembersList(target))).appendNewline()
        //        .append(Component.text("Energy: ", NamedTextColor.WHITE)).append(Component.text(target.getEnergy() + " - (", NamedTextColor.YELLOW)
        //                .append(Component.text(target.getEnergyTimeRemaining(), NamedTextColor.GOLD).append(Component.text(")", NamedTextColor.YELLOW)))).appendNewline()
        //        .append(Component.text("Level: ", NamedTextColor.WHITE)).append(Component.text(target.getLevel(), NamedTextColor.GOLD)).appendNewline();
//
//
        //if (clanRelation == ClanRelation.ENEMY) {
        //    component = component.append(Component.text("Dominance: ", NamedTextColor.WHITE)).append(Component.text(Objects.requireNonNull(playerClan).getDominanceString(target))).appendNewline();
        //}
//
        //if (client.hasRank(Rank.ADMIN)) {
        //    UtilMessage.simpleMessage(player, "Points: <yellow>%d", target.getPoints());
        //    component = component.append(Component.text("Points: ", NamedTextColor.WHITE)).append(Component.text(target.getPoints(), NamedTextColor.YELLOW));
        //}
//
        //UtilMessage.message(player, "Clans", component);

    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ClanArgumentType.CLAN.name();
        }

        return ArgumentType.NONE.name();
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
