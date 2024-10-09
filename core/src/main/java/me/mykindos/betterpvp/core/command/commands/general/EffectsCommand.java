package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javassist.runtime.Desc;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.button.DescriptionButton;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class EffectsCommand extends Command {

    private final EffectManager effectManager;

    @Inject
    public EffectsCommand(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @Override
    public String getName() {
        return "effects";
    }

    @Override
    public String getDescription() {
        return "List all effects the player currently has";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Optional<List<Effect>> listOptional = effectManager.getObject(player.getUniqueId());
        if (listOptional.isEmpty()) {
            UtilMessage.message(player, "Effects", "You do not currently have any effects applied");
            return;
        }
        List<Effect> effects = listOptional.get();
        List<Item> items = effects.stream()
                .sorted(Comparator.comparingLong(Effect::getRemainingDuration))
                .map(effect -> new DescriptionButton(effect.getDescription()))
                .collect(Collectors.toList());

        new ViewCollectionMenu("Active Effects", items, null).show(player);

    }
}