package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Singleton
@CustomLog
public class BrigadierCustomEffectCommand extends BrigadierCommand {
    private final EffectManager effectManager;
    @Inject
    public BrigadierCustomEffectCommand(ClientManager clientManager, EffectManager effectManager) {
        super(clientManager);
        this.effectManager = effectManager;
    }

    @Override
    public String getName() {
        return "customeffect";
    }

    @Override
    public String getDescription() {
        return "Apply/remove a custom effect to a player";
    }

    /**
     * Define the command, using normal rank based permissions
     *
     * @return the builder for the command
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return IBrigadierCommand.literal(getName())
                    .then(IBrigadierCommand.literal("give")
                            .then(IBrigadierCommand.argument("living entities", ArgumentTypes.entities())
                                    .then(IBrigadierCommand.argument("effect", BPvPArgumentTypes.customEffect())
                                            .executes(context -> {
                                                final CommandSender sender = context.getSource().getSender();
                                                final List<Entity> targets = context.getArgument("living entities", EntitySelectorArgumentResolver.class).resolve(context.getSource());
                                                final EffectType effectType = context.getArgument("effect", EffectType.class);
                                                doGive(sender, targets, effectType, 30, 1);
                                                return Command.SINGLE_SUCCESS;
                                            })
                                            .then(IBrigadierCommand.argument("duration  (seconds)", DoubleArgumentType.doubleArg(0.05))
                                                    .executes(context -> {
                                                        final CommandSender sender = context.getSource().getSender();
                                                        final List<Entity> targets = context.getArgument("living entities", EntitySelectorArgumentResolver.class).resolve(context.getSource());
                                                        final EffectType effectType = context.getArgument("effect", EffectType.class);
                                                        final double duration = context.getArgument("duration  (seconds)", Double.class);
                                                        doGive(sender, targets, effectType, duration, 1);
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                                    .then(IBrigadierCommand.argument("strength", IntegerArgumentType.integer(1))
                                                            .executes(context -> {
                                                                final CommandSender sender = context.getSource().getSender();
                                                                final List<Entity> targets = context.getArgument("living entities", EntitySelectorArgumentResolver.class).resolve(context.getSource());
                                                                final EffectType effectType = context.getArgument("effect", EffectType.class);
                                                                final double duration = context.getArgument("duration  (seconds)", Double.class);
                                                                final int strength = context.getArgument("strength", Integer.class);
                                                                doGive(sender, targets, effectType, duration, strength);
                                                                return Command.SINGLE_SUCCESS;
                                                            })
                                                    )
                                            )
                                    )
                            )
                    ).then(IBrigadierCommand.literal("clear")
                            .then(IBrigadierCommand.argument("living entities", ArgumentTypes.entities())
                                    .executes(context -> {
                                        final CommandSender sender = context.getSource().getSender();
                                        final List<Entity> targets = context.getArgument("living entities", EntitySelectorArgumentResolver.class).resolve(context.getSource());
                                        doClear(sender, targets, null);
                                        return Command.SINGLE_SUCCESS;
                                    })
                                    .then(IBrigadierCommand.argument("effect", BPvPArgumentTypes.customEffect())
                                            .executes(context -> {
                                                final CommandSender sender = context.getSource().getSender();
                                                final List<Entity> targets = context.getArgument("living entities", EntitySelectorArgumentResolver.class).resolve(context.getSource());
                                                final EffectType effectType = context.getArgument("effect", EffectType.class);
                                                doClear(sender, targets, effectType);
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                    );
    }

    private void doGive(CommandSender sender, @NotNull List<Entity> entities, @NotNull EffectType effectType, double duration, int strength) {
        List<LivingEntity> livingEntities = entities.stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .toList();
        livingEntities.forEach(livingEntity -> {
            effectManager.addEffect(livingEntity, effectType, strength, (long) (duration * 1000L));
        });
        Component message;
        if (livingEntities.size() <= 3 && !livingEntities.isEmpty()) {
            List<String> names = livingEntities.stream()
                    .map(livingEntity -> "<yellow>" + livingEntity.getName() + "</yellow>")
                    .toList();
            message = UtilMessage.deserialize("<yellow>%s</yellow> applied <white>%s %s</white> to %s for <green>%s</green> seconds",
                    sender.getName(), effectType.getName(), strength, UtilFormat.getConjunctiveString(names), duration);
        } else {
            message = UtilMessage.deserialize("<yellow>%s</yellow> applied <white>%s %s</white> to <green>%s</green> entities for <green>%s</green> seconds",
                    sender.getName(), effectType.getName(), strength, livingEntities.size(), duration);
        }
        clientManager.sendMessageToRank("Effect", message, Rank.HELPER);
    }

    private void doClear(CommandSender sender, @NotNull List<Entity> entities, @Nullable EffectType effectType) {
        List<LivingEntity> livingEntities = entities.stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .toList();
        livingEntities.forEach(livingEntity -> {
            if (effectType == null) {
                effectManager.removeAllEffects(livingEntity);
            } else {
                effectManager.removeEffect(livingEntity, effectType);
            }

        });
        Component message;
        if (livingEntities.size() <= 3 && !livingEntities.isEmpty()) {
            List<String> names = livingEntities.stream()
                    .map(livingEntity -> "<yellow>" + livingEntity.getName() + "</yellow>")
                    .toList();
            if (effectType == null) {
                message = UtilMessage.deserialize("<yellow>%s</yellow> removed all effects from %s",
                        sender.getName(), UtilFormat.getConjunctiveString(names));
            } else {
                message = UtilMessage.deserialize("<yellow>%s</yellow> removed <white>%s</white> from %s",
                        sender.getName(), effectType.getName(), UtilFormat.getConjunctiveString(names));
            }

        } else {
            if (effectType == null) {
                message = UtilMessage.deserialize("<yellow>%s</yellow> removed all effects from <green>%s</green> entities",
                        sender.getName(), livingEntities.size());
            } else {
                message = UtilMessage.deserialize("<yellow>%s</yellow> removed <white>%s</white> from <green>%s</green> entities",
                        sender.getName(), effectType.getName(), livingEntities.size());
            }
        }
        clientManager.sendMessageToRank("Effect", message, Rank.HELPER);
    }


}
