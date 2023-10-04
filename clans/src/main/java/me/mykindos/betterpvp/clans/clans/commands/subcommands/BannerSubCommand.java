package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class BannerSubCommand extends ClanSubCommand {

    @Inject
    private ClientManager clientManager;

    @Inject
    public BannerSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
        aliases.addAll(List.of("banner", "setclanbanner"));
    }

    @Override
    public String getName() {
        return "setbanner";
    }

    @Override
    public String getDescription() {
        return "Set your clans banner";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        // Check if player is in a clan and is a leader or admin
        Clan clan = clanManager.getClanByPlayer(player).orElse(null);
        if (clan == null) {
            UtilMessage.message(player, "Banner", "You are not in a clan!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member.getRank() != ClanMember.MemberRank.ADMIN && member.getRank() != ClanMember.MemberRank.LEADER) {
            UtilMessage.message(player, "Banner", "Only admins and leaders can set the clan banner!");
            return;
        }

        ItemStack heldItem = UtilItem.removeEnchants(player.getInventory().getItemInMainHand());
        if (!heldItem.getType().toString().endsWith("_BANNER")){
            UtilMessage.message(player, "Banner", "You must be holding a banner to set it as the clan banner!");
            return;
        }

        clan.setBanner(heldItem);
        clanManager.getRepository().updateClanBanner(clan);
        player.getInventory().removeItem(heldItem);
        UtilMessage.message(player, "Banner", "Clan banner has been set!");
    }

    @Override
    public String getArgumentType(int arg) {
        return ArgumentType.NONE.name();
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
