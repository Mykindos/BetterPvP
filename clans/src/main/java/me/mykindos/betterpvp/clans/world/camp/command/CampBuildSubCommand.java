package me.mykindos.betterpvp.clans.world.camp.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.menu.ConstructionMenu;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.blueprint.BlueprintSessions;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /clan build} opens the construction menu for your clan's camp. */
@Singleton
@SubCommand(ClanCommand.class)
public class CampBuildSubCommand extends ClanSubCommand {

    private final CampStructures structures;
    private final ConstructionService construction;
    private final StructureCatalogue catalogue;
    private final BlueprintSessions blueprints;

    @Inject
    public CampBuildSubCommand(ClanManager clanManager, ClientManager clientManager, CampStructures structures,
                               ConstructionService construction, StructureCatalogue catalogue,
                               BlueprintSessions blueprints) {
        super(clanManager, clientManager);
        this.structures = structures;
        this.construction = construction;
        this.catalogue = catalogue;
        this.blueprints = blueprints;
    }

    @Override
    public String getName() {
        return "build";
    }

    @Override
    public String getDescription() {
        return "clans.command.build.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Clan clan = clanManager.getClanByPlayer(player).orElseThrow();
        new ConstructionMenu(player, Camps.keyFor(clan), List.copyOf(structures.all()), construction, catalogue,
                blueprints, null).show(player);
    }
}
