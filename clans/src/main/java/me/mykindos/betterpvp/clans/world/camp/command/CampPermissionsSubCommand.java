package me.mykindos.betterpvp.clans.world.camp.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.menu.CampPermissionsMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import org.bukkit.entity.Player;

/** {@code /clan permissions} shows what each rank may do in the camp. Only the leader can change it. */
@Singleton
@SubCommand(ClanCommand.class)
public class CampPermissionsSubCommand extends ClanSubCommand {

    private final CampPermissions permissions;

    @Inject
    public CampPermissionsSubCommand(ClanManager clanManager, ClientManager clientManager, CampPermissions permissions) {
        super(clanManager, clientManager);
        this.permissions = permissions;
    }

    @Override
    public String getName() {
        return "permissions";
    }

    @Override
    public String getDescription() {
        return "clans.command.permissions.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Clan clan = clanManager.getClanByPlayer(player).orElseThrow();
        final boolean leader = clan.getMemberByUUID(player.getUniqueId())
                .map(member -> member.getRank() == ClanMember.MemberRank.LEADER)
                .orElse(false);
        new CampPermissionsMenu(clan.getId(), permissions, leader).show(player);
    }
}
