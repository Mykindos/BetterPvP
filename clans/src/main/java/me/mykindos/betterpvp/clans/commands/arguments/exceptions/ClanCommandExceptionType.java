package me.mykindos.betterpvp.clans.commands.arguments.exceptions;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.mykindos.betterpvp.clans.clans.Clan;

public class ClanCommandExceptionType implements CommandExceptionType {
    private final Function function;
    public ClanCommandExceptionType(Function function) {
        this.function = function;
    }

    public CommandSyntaxException create(final Clan origin) {
        return new CommandSyntaxException(this, function.apply(origin));
    }

    public CommandSyntaxException createWithContext(final ImmutableStringReader reader, final Clan origin) {
        return new CommandSyntaxException(this, function.apply(origin), reader.getString(), reader.getCursor());
    }

    public interface Function {
        Message apply(Clan origin);

    }
}
