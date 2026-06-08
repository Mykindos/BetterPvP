package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class ProtectionSubCommand extends ClanSubCommand {

    @Inject
    public ProtectionSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "protection";
    }

    @Override
    public String getDescription() {
        return "clans.command.protection.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <clan>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        Optional<Clan> targetClanOptional = clanManager.getClanByPlayer(player);
        if(targetClanOptional.isEmpty()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.protection.no-clan");
            return;
        }

       Clan clan = targetClanOptional.get();
        if(clan.isNoDominanceCooldownActive()) {
            new ConfirmationMenu(Translations.component("clans.command.clan.protection.confirmation").toString(), success -> {
                if (Boolean.TRUE.equals(success)) {
                    clan.saveProperty(ClanProperty.NO_DOMINANCE_COOLDOWN, 0L);
                    UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.protection.success");
                }
            }).show(player);
        } else {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.protection.not-active");
        }
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
