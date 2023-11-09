package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
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
        return "Apply/remove a custom effect to a player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.simpleMessage(player, "<yellow>Usage:</yellow> /customeffect <give|clear>");
    }

    @Override
    public String getArgumentType(int arg) {
        return arg == 1 ? ArgumentType.SUBCOMMAND.name() : ArgumentType.NONE.name();
    }

    @Singleton
    @SubCommand(CustomEffectCommand.class)
    private static class CustomEffectGiveSubCommand extends Command {

        @Inject
        private EffectManager effectManager;

        @Override
        public String getName() {
            return "give";
        }

        @Override
        public String getDescription() {
            return "Give a player a custom effect";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 2) {
                UtilMessage.simpleMessage(player, "<yellow>Usage:</yellow> /customeffect give <player> <effect> [duration] [amplifier]");
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                UtilMessage.message(player, "Core", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid target (does not exist)", args[0]));
            }

            EffectType effect;
            try {
                effect = EffectType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                UtilMessage.message(player, "Core", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid effect", args[1]));
                return;
            }
            int duration = 30;
            if (args.length >= 3) {
                try {
                    duration = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    UtilMessage.message(player, "Core", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid number", args[2]));
                    return;
                }

            }

            int strength = 1;
            if (args.length >= 4) {
                try {
                    strength = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    UtilMessage.message(player, "Core", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid number", args[2]));
                    return;
                }

            }
            effectManager.addEffect(player, effect, strength, duration * 1000L);
        }

        @Override
        public String getArgumentType(int arg) {
            return arg == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }

    @Singleton
    @SubCommand(CustomEffectCommand.class)
    private static class CustomEffectClearSubCommand extends Command {

        @Inject
        private EffectManager effectManager;

        @Override
        public String getName() {
            return "clear";
        }

        @Override
        public String getDescription() {
            return "Clear effects from a player";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.simpleMessage(player, "<yellow>Usage:</yellow> /customeffect clear <player> [effect]");
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                UtilMessage.message(player, "Core", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid target (does not exist)", args[0]));
            }

            if (args.length < 2) {
                effectManager.removeAllEffects(player);
                return;
            }
            EffectType effect;
            try {
                effect = EffectType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                UtilMessage.message(player, "Core", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid effect", args[1]));
                return;
            }
            effectManager.removeEffect(player, effect);
        }

        @Override
        public String getArgumentType(int arg) {
            return arg == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }


}
