package me.mykindos.betterpvp.clans.commands.arguments.exceptions;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.mykindos.betterpvp.clans.clans.Clan;

public class Clan2CommandExceptionType implements CommandExceptionType {
    private final Function function;
    public Clan2CommandExceptionType(Function function) {
        this.function = function;
    }

    public CommandSyntaxException create(final Clan origin, final Clan target) {
        return new CommandSyntaxException(this, function.apply(origin, target));
    }

    public CommandSyntaxException createWithContext(final ImmutableStringReader reader, final Clan origin, final Clan target) {
        return new CommandSyntaxException(this, function.apply(origin, target), reader.getString(), reader.getCursor());
    }

    public interface Function {
        Message apply(Clan origin, Clan target);

    }
}
