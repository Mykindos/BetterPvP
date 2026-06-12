package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Singleton
public class CustomEffectCommand extends Command {

    @Singleton
    @Override
    public String getName() {
        return "customeffect";
    }

    @Override
    public String getDescription() {
        return "core.command.custom-effect.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "core.prefix.effect", "core.command.customeffect.usage");
    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ArgumentType.SUBCOMMAND.name();
        }
        return ArgumentType.NONE.name();
    }

    @Singleton
    @SubCommand(CustomEffectCommand.class)
    private static class CustomEffectGiveSubCommand extends CustomEffectSubCommand {

        @Override
        public String getName() {
            return "give";
        }

        @Override
        public String getDescription() {
        return "core.command.custom-effect-give.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 2) {
                UtilMessage.message(player, "core.prefix.effect", "core.command.customeffect.give.usage");
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                UtilMessage.message(player, "core.prefix.effect", "core.command.customeffect.invalid_target", Component.text(args[0]));
                return;
            }

            EffectType effect = EffectTypes.getEffectTypes().stream().filter(effectType -> effectType.getName().replace("'", "")
                    .equalsIgnoreCase(args[1].replace("_", " ")))
                    .findFirst()
                    .orElse(null);
            if(effect == null) {
                UtilMessage.message(player, "core.prefix.effect", "core.command.customeffect.invalid_effect", Component.text(args[1]));
                return;
            }

            int duration = 30;
            if (args.length >= 3) {
                try {
                    duration = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    UtilMessage.message(player, "core.prefix.effect", "core.command.customeffect.invalid_number", Component.text(args[2]));
                    return;
                }

            }

            int strength = 1;
            if (args.length >= 4) {
                try {
                    strength = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    UtilMessage.message(player, "core.prefix.effect", "core.command.customeffect.invalid_number", Component.text(args[3]));
                    return;
                }

            }
            effectManager.addEffect(target, effect, strength, duration * 1000L);
            Component message = Translations.component("core.command.customeffect.give.success",
                    Component.text(player.getName()),
                    Component.text(effect.getName()),
                    Component.text(strength),
                    Component.text(target.getName()),
                    Component.text(duration));
            gamerManager.sendMessageToRank("core.prefix.effect", message, Rank.TRIAL_MOD);
        }

        @Override
        public String getArgumentType(int arg) {
            if (arg == 1) {
                return ArgumentType.PLAYER.name();
            }
            if (arg == 2) {
                return "EFFECT";
            }
            return ArgumentType.NONE.name();
        }
    }

    @Singleton
    @SubCommand(CustomEffectCommand.class)
    private static class CustomEffectClearSubCommand extends CustomEffectSubCommand {
        @Override
        public String getName() {
            return "clear";
        }

        @Override
        public String getDescription() {
        return "core.command.custom-effect-clear.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, "core.prefix.effect", "core.command.customeffect.clear.usage");
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                UtilMessage.message(player, "core.prefix.effect", "core.command.customeffect.invalid_target", Component.text(args[0]));
                return;
            }

            if (args.length < 2) {
                effectManager.removeAllEffects(target);
                Component message = Translations.component("core.command.customeffect.clear.all.success",
                        Component.text(player.getName()),
                        Component.text(target.getName()));
                gamerManager.sendMessageToRank("core.prefix.effect", message, Rank.TRIAL_MOD);
                return;
            }

            String effectName = args[1].replace("_", " ");
            EffectType effect = EffectTypes.getEffectTypes().stream().filter(effectType -> effectType.getName().replace("'", "")
                            .equalsIgnoreCase(effectName.replace("_", " ")))
                    .findFirst()
                    .orElse(null);
            if(effect == null) {
                UtilMessage.message(player, "core.prefix.effect", "core.command.customeffect.invalid_effect", Component.text(args[1]));
                return;
            }

            effectManager.removeEffect(target, effect);
            Component message = Translations.component("core.command.customeffect.clear.effect.success",
                    Component.text(player.getName()),
                    Component.text(effect.getName()),
                    Component.text(target.getName()));
            gamerManager.sendMessageToRank("core.prefix.effect", message, Rank.TRIAL_MOD);
        }

        @Override
        public String getArgumentType(int arg) {
            if (arg == 1) {
                return ArgumentType.PLAYER.name();
            }
            if (arg == 2) {
                return "EFFECT";
            }
            return ArgumentType.NONE.name();
        }
    }
}
