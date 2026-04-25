package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.entity.Player;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.List;

import static me.mykindos.betterpvp.clans.database.jooq.Tables.CLANS;
import static me.mykindos.betterpvp.clans.database.jooq.Tables.CLAN_METADATA;
import static me.mykindos.betterpvp.clans.database.jooq.Tables.CLAN_PROPERTIES;
import static me.mykindos.betterpvp.clans.database.jooq.Tables.CLAN_TERRITORY;
import static me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator.ID_GENERATOR;

@CustomLog
@Singleton
@SubCommand(ClanCommand.class)
public class CopyAdminClansSubCommand extends ClanSubCommand {

    private final Database database;

    @Inject
    public CopyAdminClansSubCommand(ClanManager clanManager, ClientManager clientManager, Database database) {
        super(clanManager, clientManager);
        this.database = database;
    }

    @Override
    public String getName() {
        return "copyadminclans";
    }

    @Override
    public String getDescription() {
        return "Copy all admin clans to another realm";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <realmId>";
    }

    @Override
    public void execute(Player player, Client client, String[] args) {
        if (args.length < 1) {
            UtilMessage.message(player, "Clans", "Usage: " + getUsage());
            return;
        }

        try {
            int realmId = Integer.parseInt(args[0]);
            List<Clan> adminClans = clanManager.getObjects().values().stream().filter(Clan::isAdmin).toList();

            if (adminClans.isEmpty()) {
                UtilMessage.message(player, "Clans", "No admin clans found to copy.");
                return;
            }

            database.getAsyncDslContext().executeAsyncVoid(ctx -> {

                ctx.transaction(config -> {
                    DSLContext ctxl = DSL.using(config);
                    int count = 0;
                    for (Clan clan : adminClans) {
                        long newId = ID_GENERATOR.nextId();

                        ClanCore core = clan.getCore();
                        // Save to CLANS
                        ctxl.insertInto(CLANS)
                                .set(CLANS.ID, newId)
                                .set(CLANS.REALM, realmId)
                                .set(CLANS.NAME, clan.getName())
                                .set(CLANS.HOME, core.getPosition() == null ? "" : UtilWorld.locationToString(core.getPosition(), false))
                                .set(CLANS.ADMIN, 1)
                                .set(CLANS.SAFE, clan.isSafe() ? 1 : 0)
                                .execute();

                        // Save to CLAN_METADATA
                        ctxl.insertInto(CLAN_METADATA)
                                .set(CLAN_METADATA.CLAN, newId)
                                .set(CLAN_METADATA.BANNER, "")
                                .set(CLAN_METADATA.VAULT, "")
                                .set(CLAN_METADATA.MAILBOX, "")
                                .execute();

                        // Copy territory
                        for (var territory : clan.getTerritory()) {
                            ctxl.insertInto(CLAN_TERRITORY)
                                    .set(CLAN_TERRITORY.CLAN, newId)
                                    .set(CLAN_TERRITORY.CHUNK, territory.getChunk())
                                    .execute();
                        }

                        // Copy properties
                        clan.getProperties().getMap().forEach((key, value) -> {
                            ctxl.insertInto(CLAN_PROPERTIES)
                                    .set(CLAN_PROPERTIES.CLAN, newId)
                                    .set(CLAN_PROPERTIES.PROPERTY, key)
                                    .set(CLAN_PROPERTIES.VALUE, value.toString())
                                    .execute();
                        });

                        count++;
                    }

                    final int finalCount = count;
                    player.sendMessage(UtilMessage.deserialize("<green>Successfully copied <white>%d <green>admin clans to realm <white>%s", finalCount, realmId));
                });
            }).exceptionally(ex -> {
                UtilMessage.message(player, "Clans", "Failed to copy admin clans: " + ex.getMessage());
                log.error("Failed to copy admin clans with exception {}", ex.getMessage(), ex).submit();
                return null;
            });
        } catch (NumberFormatException ex) {
            UtilMessage.message(player, "Clans", "Invalid realmId");
        }
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
