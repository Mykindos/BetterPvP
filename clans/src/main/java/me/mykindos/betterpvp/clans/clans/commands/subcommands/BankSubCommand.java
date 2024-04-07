package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@CustomLog
@Singleton
@SubCommand(ClanCommand.class)
public class BankSubCommand extends ClanSubCommand {

    @Inject
    public BankSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "bank";
    }

    @Override
    public String getDescription() {
        return "View your clans bank";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " [deposit|withdraw]";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

        if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.MEMBER)) {
            UtilMessage.message(player, "Clans", "Recruits cannot view the clan bank");
            return;
        }

       UtilMessage.simpleMessage(player, "Clans", "<yellow>Bank balance: <green>$%d", clan.getBalance());

    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }

    @Override
    public String getArgumentType(int arg) {
        return arg == 1 ? ArgumentType.SUBCOMMAND.name() : ArgumentType.NONE.name();
    }

    @Singleton
    @SubCommand(BankSubCommand.class)
    private static class BankWithdrawSubCommand extends ClanSubCommand {

        @Inject
        public BankWithdrawSubCommand(ClanManager clanManager, ClientManager clientManager) {
            super(clanManager, clientManager);
        }

        @Override
        public String getName() {
            return "withdraw";
        }

        @Override
        public String getDescription() {
            return "Withdraw money from your clans bank";
        }

        @Override
        public String getUsage() {
            return super.getUsage() + " <amount>";
        }

        @Override
        public void execute(Player player, Client client, String... args) {

            if(args.length != 2) {
                UtilMessage.message(player, "Clans", "Correct usage /clan bank withdraw <amount>");
                return;
            }

            Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

            if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
                UtilMessage.message(player, "Clans", "Only clan admins can withdraw money from the clan bank");
                return;
            }

            if(clan.isAlmostPillaged()) {
                UtilMessage.message(player, "Clans", "You cannot withdraw money from your clans bank if your clan is close to being pillaged.");
                return;
            }

            Gamer gamer = client.getGamer();
            try {
                int amountToWithdraw = Integer.parseInt(args[1]);
                if(clan.getBalance() < amountToWithdraw) {
                    UtilMessage.message(player, "Clans", "You cannot withdraw more money than your clans bank has available.");
                    return;
                }

                if(amountToWithdraw <= 0) {
                    UtilMessage.message(player, "Clans", "You must specify a value greater than 0.");
                    return;
                }


                clan.saveProperty(ClanProperty.BALANCE, clan.getBalance() - amountToWithdraw);
                gamer.saveProperty(GamerProperty.BALANCE, gamer.getBalance() + amountToWithdraw);

                clan.messageClan("<yellow>" + player.getName() + " <gray>withdrew <green>$" + amountToWithdraw + " <gray>from the clan bank.", null, true);
                log.info("{} withdrew ${} from clan {}", player.getName(), amountToWithdraw, clan.getId().toString());
            }catch(NumberFormatException ex) {
                UtilMessage.message(player, "Clans", "Value provided is not a valid number.");
            }

        }

        @Override
        public ClanMember.MemberRank getRequiredMemberRank() {
            return ClanMember.MemberRank.ADMIN;
        }

        @Override
        public String getArgumentType(int arg) {
            return ArgumentType.NONE.name();
        }

    }

    @Singleton
    @SubCommand(BankSubCommand.class)
    private static class BankDepositSubCommand extends ClanSubCommand {

        @Inject
        public BankDepositSubCommand(ClanManager clanManager, ClientManager clientManager) {
            super(clanManager, clientManager);
        }

        @Override
        public String getName() {
            return "deposit";
        }

        @Override
        public String getDescription() {
            return "Deposit money into your clans bank";
        }

        @Override
        public String getUsage() {
            return super.getUsage() + " <amount>";
        }

        @Override
        public void execute(Player player, Client client, String... args) {

            if(args.length != 2) {
                UtilMessage.message(player, "Clans", "Correct usage /clan bank deposit <amount>");
                return;
            }

            Clan clan = clanManager.getClanByPlayer(player).orElseThrow();
            Gamer gamer = client.getGamer();
            try {
                int amountToWithdraw = Integer.parseInt(args[1]);
                if(gamer.getBalance() < amountToWithdraw) {
                    UtilMessage.message(player, "Clans", "You cannot deposit more money than you have available.");
                    return;
                }

                if(amountToWithdraw <= 0) {
                    UtilMessage.message(player, "Clans", "You must specify a value greater than 0.");
                    return;
                }

                clan.saveProperty(ClanProperty.BALANCE, clan.getBalance() + amountToWithdraw);
                gamer.saveProperty(GamerProperty.BALANCE, gamer.getBalance() - amountToWithdraw);

                clan.messageClan("<yellow>" + player.getName() + " <gray>deposited <green>$" + amountToWithdraw + " <gray>into the clan bank.", null, true);
                log.info("{} deposited ${} into clan {}", player.getName(), amountToWithdraw, clan.getId().toString());
            }catch(NumberFormatException ex) {
                UtilMessage.message(player, "Clans", "Value provided is not a valid number.");
            }

        }

        @Override
        public String getArgumentType(int arg) {
            return ArgumentType.NONE.name();
        }

    }

}
