package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanCreateEvent;
import me.mykindos.betterpvp.core.chat.filter.IFilterService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

import static me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator.ID_GENERATOR;

@Singleton
@SubCommand(ClanCommand.class)
public class CreateClanSubCommand extends ClanSubCommand {

    @Inject
    @Config(path = "command.clan.create.maxCharactersInClanName", defaultValue = "13")
    private int maxCharactersInClanName;

    @Inject
    @Config(path = "command.clan.create.minCharactersInClanName", defaultValue = "3")
    private int minCharactersInClanName;

    private final Clans clans;
    private final IFilterService filterService;

    @Inject
    public CreateClanSubCommand(ClanManager clanManager, ClientManager clientManager, Clans clans, IFilterService filterService) {
        super(clanManager, clientManager);
        this.clans = clans;
        this.filterService = filterService;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "clans.command.create-clan.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <name>";
    }

    @Override
    public void execute(Player player, Client client, String[] args) {
        if (args.length == 0) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.create.no-args");
            return;
        }

        if (clanManager.getClanByClient(client).isPresent()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.create.already-in-clan");
            return;
        }

        String clanName = args[0];

        if (clanName.length() < minCharactersInClanName) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.create.too-short", Component.text(minCharactersInClanName, NamedTextColor.YELLOW));
            return;
        }

        if (clanName.length() > maxCharactersInClanName) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.create.too-long", Component.text(maxCharactersInClanName, NamedTextColor.YELLOW));
            return;
        }

        if (clanName.matches("^.*[^a-zA-Z0-9].*$")) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.create.invalid-chars");
            return;
        }

        filterService.isFiltered(clanName).thenAcceptAsync(filtered -> {
            if (filtered) {
                UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.create.filtered");
            } else {
                Optional<Clan> clanOptional = clanManager.getClanByName(clanName.toLowerCase());
                if (clanOptional.isEmpty()) {
                    Clan clan = new Clan(ID_GENERATOR.nextId());
                    clan.setName(clanName);
                    clan.setOnline(true);
                    clan.getProperties().registerListener(clan);

                    UtilServer.callEvent(new ClanCreateEvent(player, clan));
                } else {
                    UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.create.already-exists");
                }
            }
        }, Bukkit.getScheduler().getMainThreadExecutor(clans));


    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }

}
