package me.mykindos.betterpvp.clans.clans.commands.subcommands;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Singleton
@SubCommand(ClanCommand.class)
public class FarmingInfoSubCommand extends ClanSubCommand {

    private final int baseFarmingY;
    private final int baseFarmingLevels;
    private final boolean offlineGrowing;

    @Inject
    public FarmingInfoSubCommand(ClanManager clanManager, ClientManager clientManager, Clans clans) {
        super(clanManager, clientManager);

        this.aliases.add("farminglevels");
        baseFarmingY = clans.getConfig().getInt("clans.farming.baseY", 0);
        baseFarmingLevels = clans.getConfig().getInt("clans.farming.baseFarmingLevels", 0);
        offlineGrowing = clans.getConfig().getBoolean("clans.farming.allowOfflineGrowing", false);
    }

    @Override
    public String getName() {
        return "farminginfo";
    }

    @Override
    public String getDescription() {
        return "See information about your clan's farming capabilities.";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final @NotNull Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

        final int minY = ClanPerkManager.getInstance().computeFarmingLevelMinY(clan, baseFarmingY, baseFarmingLevels);

        Component message = Component.text("Farming Info:")
                .append(Component.newline())
                .append(Component.text("Farming Y Levels: ", NamedTextColor.WHITE))
                .append(Component.text( baseFarmingY + " to " + minY, NamedTextColor.GREEN));

        final String offlineGrowingText = offlineGrowing ? "No" : "Yes";
        final NamedTextColor offlineGrowingColor = offlineGrowing ? NamedTextColor.RED : NamedTextColor.GREEN;

        message = message
                .append(Component.newline())
                .append(Component.text("Clan must be online for crops to grow: ", NamedTextColor.WHITE))
                .append(Component.text(offlineGrowingText, offlineGrowingColor));

        UtilMessage.message(player, "Clans", message);
    }
}
