package me.mykindos.betterpvp.clans.clans.commands;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class ClanSubCommand extends Command {

    protected final ClanManager clanManager;
    protected final GamerManager gamerManager;

    public ClanSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        this.clanManager = clanManager;
        this.gamerManager = gamerManager;
    }

    public String getUsage() {
        return getName();
    }

    @Override
    public void process(Player player, Client client, String... args) {
        if (!canExecuteWithoutClan()) {
            Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(player);
            if (playerClanOptional.isEmpty()) {
                UtilMessage.message(player, "Clans", "You are not in a clan");
                return;
            }
        }

        if (requiresServerAdmin()) {
            Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId());
            if (gamerOptional.isPresent()) {
                Gamer gamer = gamerOptional.get();
                if (!gamer.getClient().hasRank(Rank.ADMIN)) {
                    return;
                }
            }
        }

        execute(player, client, args);
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();
        if (args.length == 0) return tabCompletions;

        String lowercaseArg = args[args.length - 1].toLowerCase();
        switch (getArgumentType(args.length)) {
            case "PLAYER" ->
                    tabCompletions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().
                            startsWith(lowercaseArg)).toList());
            case "POSITION_X" ->
                    tabCompletions.add(sender instanceof Player player ? player.getLocation().getX() + "" : "0");
            case "POSITION_Y" ->
                    tabCompletions.add(sender instanceof Player player ? player.getLocation().getY() + "" : "0");
            case "POSITION_Z" ->
                    tabCompletions.add(sender instanceof Player player ? player.getLocation().getZ() + "" : "0");
            case "BOOLEAN" -> tabCompletions.addAll(List.of("true", "false"));
            case "CLAN" -> clanManager.getObjects().forEach((key, value) -> {
                if (key.toLowerCase().startsWith(lowercaseArg)) {
                    tabCompletions.add(key);
                }
            });
            case "CLAN_MEMBER" -> {
                if (sender instanceof Player player) {
                    Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
                    clanOptional.ifPresent(clan -> clan.getMembers().forEach(clanMember -> {
                        Optional<Gamer> gamerOptional = gamerManager.getObject(clanMember.getUuid());
                        gamerOptional.ifPresent(gamer -> {
                            if (gamer.getClient().getName().toLowerCase().startsWith(lowercaseArg)) {
                                tabCompletions.add(gamer.getClient().getName());
                            }
                        });
                    }));
                }
            }
            case "SUBCOMMAND" -> getSubCommands().forEach(subCommand -> {
                if (subCommand.showTabCompletion(sender)) {
                    if (subCommand.getName().toLowerCase().startsWith(lowercaseArg)) {
                        tabCompletions.add(subCommand.getName());
                        tabCompletions.addAll(subCommand.getAliases());
                    }
                }
            });
        }


        return tabCompletions;

    }

    protected enum ClanArgumentType {
        CLAN,
        CLAN_MEMBER
    }

    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.RECRUIT;
    }

    public boolean requiresServerAdmin() {
        return false;
    }

    public boolean canExecuteWithoutClan() {
        return false;
    }

    @Override
    public boolean showTabCompletion(CommandSender sender) {
        if (sender instanceof Player player) {

            if (requiresServerAdmin()) {
                Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId());
                if (gamerOptional.isPresent()) {
                    Gamer gamer = gamerOptional.get();
                    if (!gamer.getClient().hasRank(Rank.ADMIN)) return false;
                }
            }

            Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
            if (clanOptional.isPresent()) {
                Clan clan = clanOptional.get();
                ClanMember member = clan.getMember(player.getUniqueId());
                return member.getRank().getPrivilege() >= getRequiredMemberRank().getPrivilege();
            } else {
                return canExecuteWithoutClan();
            }
        }

        return true;
    }

}
