package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

@Singleton
public class ClanCommand extends Command {

    private final ClanManager clanManager;
    private final GamerManager gamerManager;

    @WithReflection
    @Inject
    public ClanCommand(ClanManager clanManager, GamerManager gamerManager) {
        this.clanManager = clanManager;
        this.gamerManager = gamerManager;

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
        if (args.length != 0) return;

        clanManager.getClanByPlayer(player).ifPresentOrElse(clan -> {

                    Component component = Component.text(clan.getName() + " Information: ", NamedTextColor.AQUA).appendNewline()
                            .append(Component.text("Age: ", NamedTextColor.WHITE)).append(Component.text(clan.getAge(), NamedTextColor.YELLOW)).appendNewline()
                            .append(Component.text("Territory: ", NamedTextColor.WHITE)).append(Component.text(clan.getTerritory().size() + "/" + (3 + clan.getMembers().size()), NamedTextColor.YELLOW)).appendNewline()
                            .append(Component.text("Home: ", NamedTextColor.WHITE)).append(UtilMessage.getMiniMessage((clan.getHome() == null ? "<red>Not set" : "<yellow>" + UtilWorld.locationToString(clan.getHome())))).appendNewline()
                            .append(Component.text("Allies: ", NamedTextColor.WHITE)).append(UtilMessage.getMiniMessage(clanManager.getAllianceList(player, clan))).appendNewline()
                            .append(Component.text("Enemies: ", NamedTextColor.WHITE)).append(UtilMessage.getMiniMessage(clanManager.getEnemyListDom(player, clan))).appendNewline()
                            .append(Component.text("Members: ", NamedTextColor.WHITE)).append(UtilMessage.getMiniMessage(clanManager.getMembersList(clan))).appendNewline()
                            .append(Component.text("Cooldown: ", NamedTextColor.WHITE)).append(UtilMessage.getMiniMessage((!clan.isNoDominanceCooldownActive() ? "<green>No"
                                    : "<red>" + UtilTime.getTime(clan.getNoDominanceCooldown() - System.currentTimeMillis(), UtilTime.TimeUnit.BEST, 1)))).appendNewline()
                            .append(Component.text("Energy: ", NamedTextColor.WHITE)).append(Component.text(clan.getEnergy() + " - (", NamedTextColor.YELLOW)
                                    .append(Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.GOLD).append(Component.text(")", NamedTextColor.YELLOW)))).appendNewline()
                            .append(Component.text("Level: ", NamedTextColor.WHITE)).append(Component.text(clan.getLevel(), NamedTextColor.GOLD)).appendNewline()
                            .append(Component.text("Points: ", NamedTextColor.WHITE)).append(Component.text(clan.getPoints(), NamedTextColor.YELLOW));

                    UtilMessage.message(player, "Clans", component);


                },
                () -> UtilMessage.message(player, "Clans", "You are not in a clan")
        );

    }
}
