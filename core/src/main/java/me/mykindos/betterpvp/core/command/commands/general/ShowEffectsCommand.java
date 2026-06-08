package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ShowEffectsCommand extends Command {

    private final EffectManager effectManager;

    @Inject
    public ShowEffectsCommand(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @Override
    public String getName() {
        return "showeffects";
    }

    @Override
    public String getDescription() {
        return "core.command.show-effects.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Optional<ConcurrentHashMap<String, List<Effect>>> listOptional = effectManager.getObject(player.getUniqueId().toString());
        if (listOptional.isEmpty()) {
            UtilMessage.message(player, "core.prefix.effects", "core.command.showeffects.none");
            return;
        }
        ConcurrentHashMap<String, List<Effect>> effects = listOptional.get();
        effects.values().forEach(effectList -> {
            effectList.forEach(effect -> {
                UtilMessage.message(player, "core.prefix.effects", "core.command.showeffects.entry",
                        Component.text(effect.getEffectType().getName() + " " + effect.getAmplifier(), NamedTextColor.WHITE),
                        Component.text(UtilTime.getTime(effect.getRemainingDuration(), 1), NamedTextColor.GREEN));
            });
        });

    }
}
