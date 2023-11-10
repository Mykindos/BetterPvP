package me.mykindos.betterpvp.clans.fields.commands.subcommands;

import com.google.common.collect.SetMultimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.fields.Fields;
import me.mykindos.betterpvp.clans.fields.commands.FieldsCommand;
import me.mykindos.betterpvp.clans.fields.model.FieldsBlock;
import me.mykindos.betterpvp.clans.fields.model.FieldsInteractable;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Set;

@Singleton
@SubCommand(FieldsCommand.class)
public class FieldsInfoSubCommand extends Command {

    @Inject
    private ClanManager clanManager;

    @Inject
    private Fields fields;

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Display information on Fields.";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Set<FieldsInteractable> types = fields.getBlockTypes();
        final SetMultimap<FieldsInteractable, FieldsBlock> blocks = fields.getBlocks();
        final long activeOres = blocks.values().stream().filter(FieldsBlock::isActive).count();
        final long inactiveOres = blocks.values().size() - activeOres;
        final double modifier = fields.getSpeedBuff();

        UtilMessage.simpleMessage(player, "Fields", "Fields information:");
        UtilMessage.simpleMessage(player, "Fields", "Active blocks: <alt2>%s",activeOres);
        UtilMessage.simpleMessage(player, "Fields", "Inactive blocks: <alt2>%s",inactiveOres);
        UtilMessage.simpleMessage(player, "Fields", "Player count buff: <alt2>%sx",modifier);
        UtilMessage.simpleMessage(player, "Fields", "");
        UtilMessage.simpleMessage(player, "Fields", "Blocks:");

        for (FieldsInteractable type : types) {
            final Collection<FieldsBlock> typeOres = fields.getBlocks(type);
            final long total = typeOres.size();
            final long active = typeOres.stream().filter(FieldsBlock::isActive).count();

            UtilMessage.simpleMessage(player,
                    "Fields",
                    "- <alt2>%s</alt2> (<alt>%s</alt>/<alt>%s</alt>) (<alt>%s</alt>s)",
                    type.getName(),
                    active,
                    total,
                    type.getRespawnDelay() / modifier
            );
        }
    }

}
