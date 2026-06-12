package me.mykindos.betterpvp.champions.stats.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.stats.impl.ChampionsFilter;
import me.mykindos.betterpvp.champions.stats.repository.ChampionsStatsRepository;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.stats.impl.GlobalCombatStatsRepository;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
@CustomLog
public class CombatCommand extends Command {

    @Inject
    private Champions champions;

    @Inject
    private GlobalCombatStatsRepository globalRepository;

    @Inject
    private ChampionsStatsRepository championsRepository;

    @Inject
    private RoleManager roleManager;

    @Inject
    private ClientManager clientManager;

    @Override
    public String getName() {
        return "combat";
    }

    @Override
    public String getDescription() {
        return "champions.command.combat.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length > 2) {
            UtilMessage.message(player, "core.prefix.combat", "champions.combat.usage", Component.text("/combat [role] [player]", NamedTextColor.YELLOW));
            return;
        }

        if (args.length > 1) {

            clientManager.search(player).offline(args[1]).thenAcceptAsync(result -> {
                result.ifPresent(target -> run(player, target, args));
            });

        } else {
            UtilServer.runTaskAsync(JavaPlugin.getPlugin(Champions.class), () -> run(player, client, args));
        }
    }

    private void run(Player caster, Client target, String[] args) {
        CompletableFuture<CombatData> loaded;
        final ChampionsFilter filter;
        try {
            if (args.length == 0) {
                final Player player = target.getGamer().getPlayer();
                filter = player == null || !player.isValid() ? ChampionsFilter.NONE : ChampionsFilter.fromRole(roleManager.getRole(player));
                loaded = championsRepository.getDataAsync(target.getUniqueId()).thenApply(roleStats -> roleStats.getCombatData(filter));
            } else {
                filter = ChampionsFilter.valueOf(args[0].toUpperCase());
                if (filter == ChampionsFilter.GLOBAL) {
                    // For some reason needs to be cast to CombatData even though it's a subtype?
                    loaded = globalRepository.getDataAsync(target.getUniqueId()).thenApply(global -> global);
                } else {
                    loaded = championsRepository.getDataAsync(target.getUniqueId()).thenApply(roleStats -> roleStats.getCombatData(filter));
                }
            }
        } catch (IllegalArgumentException exception) {
            UtilMessage.message(caster, "core.prefix.combat", "champions.combat.invalid-role");
            return;
        }

        if (!loaded.isDone()) {
            UtilMessage.message(caster, "core.prefix.combat", "champions.combat.retrieving");
        }

        final String targetName = target.getName();
        loaded.whenComplete((data, throwable) -> {
            if (throwable != null) {
                UtilMessage.message(caster, "core.prefix.combat", "champions.combat.error");
                log.error("There was an error retrieving player data for {}", targetName, throwable).submit();
                return;
            }

            UtilMessage.message(caster, "core.prefix.combat", "champions.combat.data-for", Component.text(targetName, NamedTextColor.YELLOW));
            UtilMessage.message(caster, "core.prefix.combat", "champions.combat.type", Component.text(filter.getName(), NamedTextColor.GREEN));
            for (Component component : data.getDescription()) {
                UtilMessage.message(caster, "core.prefix.combat", component);
            }
        });
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(ChampionsFilter.values()).filter(championsFilter -> championsFilter.name().toLowerCase().contains(args[0].toLowerCase())).
                    map(filter -> filter.name().toLowerCase()).toList();
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().contains(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
