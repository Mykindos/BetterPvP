package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;

@Singleton
public abstract class BrigadierClanSubCommand extends ClanBrigadierCommand {
    @Inject
    protected BrigadierClanSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    @Override
    public boolean requirement(CommandSourceStack source) {
        return super.requirement(source) && senderHasClanRank(source);
    }

    @Override
    public Component getRequirementComponent(CommandContext<CommandSourceStack> context) {
        Component component = super.getRequirementComponent(context);
        final Optional<Clan> clanOptional = clanManager.getClanByPlayer(Objects.requireNonNull(context.getSource().getExecutor()).getUniqueId());
        final boolean inClan = clanOptional.isPresent();
        boolean hasClanRank = false;
        if (clanOptional.isPresent()) {
            final Clan clan = clanOptional.get();
            hasClanRank = clan.getMember(Objects.requireNonNull(context.getSource().getExecutor()).getUniqueId()).hasRank(this.requiredMemberRank());
        }
        component = component.appendNewline();
        component = component.append(Component.text("Need a Clan: ", NamedTextColor.WHITE))
                .append(Component.text(true, NamedTextColor.GREEN).append(Component.text(" | ", NamedTextColor.GRAY))
                .append((Component.text("You: ", NamedTextColor.WHITE))
                        .append(Component.text(inClan, inClan? NamedTextColor.GREEN : NamedTextColor.RED))));
        component = component.appendNewline();
        component = component.append(Component.text("Clan Rank: ", NamedTextColor.WHITE))
                .append(Component.text(requiredMemberRank().name(), NamedTextColor.GREEN).append(Component.text(" | ", NamedTextColor.GRAY))
                        .append((Component.text("You Qualify: ", NamedTextColor.WHITE))
                                .append(Component.text(hasClanRank, hasClanRank ? NamedTextColor.GREEN : NamedTextColor.RED))));
        return component;
    }

    protected abstract ClanMember.MemberRank requiredMemberRank();

    protected boolean senderHasClanRank(CommandSourceStack stack) {
        final CommandSender sender = stack.getSender();
        if (sender.isOp()) return true;
        if (!(sender instanceof final Player player)) return true;

        //Always allow admins (for execute)
        Client client = clientManager.search().online(player);
        if (client.hasRank(Rank.ADMIN) || client.isAdministrating()) return true;

        final Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if (clanOptional.isEmpty()) return false;
        final Clan clan = clanOptional.get();
        return clan.getMember(player.getUniqueId()).hasRank(requiredMemberRank());
    }
}
