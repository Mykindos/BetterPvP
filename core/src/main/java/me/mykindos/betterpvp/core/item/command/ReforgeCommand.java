package me.mykindos.betterpvp.core.item.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.purity.bias.PurityReforgeBiasRegistry;
import me.mykindos.betterpvp.core.item.reforging.GuiReforge;
import org.bukkit.entity.Player;

@Singleton
public class ReforgeCommand extends Command {

    private final ItemFactory itemFactory;
    private final PurityReforgeBiasRegistry biasRegistry;

    @Inject
    public ReforgeCommand(ItemFactory itemFactory, PurityReforgeBiasRegistry biasRegistry) {
        this.itemFactory = itemFactory;
        this.biasRegistry = biasRegistry;
    }

    @Override
    public String getName() {
        return "reforge";
    }

    @Override
    public String getDescription() {
        return "Admin command to reforge items";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        GuiReforge gui = new GuiReforge(player, itemFactory, biasRegistry);
        gui.show(player);
    }

}
