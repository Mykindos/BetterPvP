package me.mykindos.betterpvp.core.command.brigadier;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralArgumentBuilder;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralCommandNode;

@CustomLog
public abstract class CustomBrigadierCommand extends BrigadierCommand {
    protected CustomBrigadierCommand(ClientManager clientManager) {
        super(clientManager);
    }

    @Override
    public abstract BPvPLiteralArgumentBuilder define();

    @Override
    public BPvPLiteralCommandNode build() {
        BPvPLiteralArgumentBuilder root = define();
        this.getChildren().forEach(child -> {
            if (child instanceof CustomBrigadierCommand testCommand) {
                log.info("Defining test child: {}", child.getName()).submit();
                BPvPLiteralArgumentBuilder childArgument = testCommand.define().requires(child::requirement);
                //define the child
                root.then(childArgument);
            }


            /*//add all aliases like Paper does for the root
            for (String alias : child.getAliases()) {
                log.info("Adding test alias: {}", alias).submit();
                root.then(IBrigadierCommand.copyLiteral(alias, childArgument));
            }*/
        });
        BPvPLiteralCommandNode rootNode = root.requires(this::requirement).build();
        return rootNode;
    }
}
