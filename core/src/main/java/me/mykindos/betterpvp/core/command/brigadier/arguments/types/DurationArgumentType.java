package me.mykindos.betterpvp.core.command.brigadier.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.mykindos.betterpvp.core.command.brigadier.arguments.ArgumentException;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.jetbrains.annotations.NotNull;

/**
 * A length of time typed as a number and a unit, such as {@code 30m} or {@code 2d}, or {@code perm}. Converts to
 * milliseconds, with {@code perm} as -1. Zero and lengths too long for a {@code long} are rejected. Offers no suggestions, so the client shows the argument name as a hint.
 */
@Singleton
public class DurationArgumentType extends BPvPArgumentType<Long, String> implements CustomArgumentType.Converted<@NotNull Long, @NotNull String> {

    private static final Pattern DURATION = Pattern.compile("(\\d+)([ydhms])", Pattern.CASE_INSENSITIVE);

    @Inject
    protected DurationArgumentType() {
        super("Duration");
    }

    @Override
    public @NotNull Long convert(@NotNull String nativeType) throws CommandSyntaxException {
        if (nativeType.equalsIgnoreCase("perm")) {
            return -1L;
        }
        final Matcher matcher = DURATION.matcher(nativeType);
        if (!matcher.matches()) {
            throw ArgumentException.INVALID_DURATION.create(nativeType);
        }
        try {
            final long amount = Long.parseLong(matcher.group(1));
            if (amount <= 0) {
                throw ArgumentException.INVALID_DURATION.create(nativeType);
            }
            return Math.multiplyExact(amount, UtilTime.parseTimeString("1", matcher.group(2)));
        } catch (NumberFormatException | ArithmeticException e) {
            throw ArgumentException.INVALID_DURATION.create(nativeType);
        }
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}
