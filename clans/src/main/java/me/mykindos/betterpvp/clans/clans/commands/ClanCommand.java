package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Singleton
public class ClanCommand extends Command {

    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @WithReflection
    @Inject
    public ClanCommand(ClanManager clanManager, ClientManager clientManager) {
        this.clanManager = clanManager;
        this.clientManager = clientManager;

        aliases.addAll(List.of("c", "f", "faction"));
    }

    @Override
    public String getName() {
        return "clan";
    }

    @Override
    public String getDescription() {
        return "Basic clan command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(player);
        Clan playerClan = playerClanOptional.orElse(null);

        // If no arguments, show the clan of the current player
        if (args.length == 0) {
            if (playerClan != null) {
                final Gamer gamer = clientManager.search().online(player).getGamer();
                Optional<Boolean> clanMenuEnabled = gamer.getProperty(GamerProperty.CLAN_MENU_ENABLED);
                if (clanMenuEnabled.isPresent()) {
                    if (clanMenuEnabled.get()) {
                        openClanMenu(player, playerClan, playerClan);
                    } else {
                        displayChat(player, playerClan);
                    }
                }

            } else {
                UtilMessage.message(player, "Clans", "You are not in a clan");
            }

            return;
        }


        // If there's an argument, first try to get the clan by name
        Optional<Clan> clanByName = clanManager.getClanByName(args[0]);
        if (clanByName.isPresent()) {
            openClanMenu(player, playerClan, clanByName.get());
            return;
        }

        // If the clan was not found by name, try to get the clan by player name
        final Collection<Client> matches = clientManager.search(player).inform(false).advancedOnline(args[0]);
        if (matches.size() == 1) {
            final Client found = matches.iterator().next();
            final Optional<Clan> foundClan = clanManager.getClanByPlayer(found.getUniqueId());
            foundClan.ifPresentOrElse(clan -> openClanMenu(player, playerClan, clan), () -> {
                UtilMessage.message(player, "Clans", "That player is not in a clan.");
            });
        } else {
            UtilMessage.message(player, "Clans", "Cannot find the specified clan or player.");
        }
    }

    private void openClanMenu(Player player, Clan playerClan, Clan clan) {
        new ClanMenu(player, playerClan, clan).show(player);
    }

    public void displayChat(Player player, Clan clan) {
        Component component = Component.text(clan.getName() + " Information: ", NamedTextColor.AQUA).appendNewline()
                .append(Component.text("Age: ", NamedTextColor.WHITE)).append(Component.text(clan.getAge(), NamedTextColor.YELLOW)).appendNewline()
                .append(Component.text("Territory: ", NamedTextColor.WHITE)).append(Component.text(clan.getTerritory().size() + "/" + (3 + clan.getMembers().size()), NamedTextColor.YELLOW)).appendNewline()
                .append(Component.text("Home: ", NamedTextColor.WHITE)).append(UtilMessage.getMiniMessage((clan.getHome() == null ? "<red>Not set" : "<yellow>" + UtilWorld.locationToString(clan.getHome())))).appendNewline()
                .append(Component.text("Allies: ", NamedTextColor.WHITE)).append(UtilMessage.getMiniMessage(clanManager.getAllianceList(player, clan))).appendNewline()
                .append(Component.text("Enemies: ", NamedTextColor.WHITE)).append(UtilMessage.getMiniMessage(clanManager.getEnemyListDom(player, clan))).appendNewline()
                .append(Component.text("Members: ", NamedTextColor.WHITE)).append(UtilMessage.getMiniMessage(clanManager.getMembersList(clan))).appendNewline()
                .append(Component.text("Cooldown: ", NamedTextColor.WHITE)).append(UtilMessage.getMiniMessage((!clan.isNoDominanceCooldownActive() ? "<green>No"
                        : "<red>" + UtilTime.getTime((clan.getNoDominanceCooldown() - System.currentTimeMillis()), 1)))).appendNewline()
                .append(Component.text("Energy: ", NamedTextColor.WHITE)).append(Component.text(clan.getEnergy() + " - (", NamedTextColor.YELLOW)
                        .append(Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.GOLD).append(Component.text(")", NamedTextColor.YELLOW)))).appendNewline()
                .append(Component.text("Level: ", NamedTextColor.WHITE)).append(Component.text(clan.getLevel(), NamedTextColor.GOLD)).appendNewline()
                .append(Component.text("Points: ", NamedTextColor.WHITE)).append(Component.text(clan.getPoints(), NamedTextColor.YELLOW));

        UtilMessage.message(player, "Clans", component);
    }
}
