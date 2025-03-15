package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;
import org.jetbrains.annotations.NotNull;

/**
 * Shows a prompt message that this is a ClanName. Enforces that the returned name contains valid characters and is correct length
 */
@Singleton
public class ClanNameArgument extends BPvPArgumentType<String, String> implements CustomArgumentType.Converted<String, String> {

    private final ClanManager clanManager;

    @Inject
    public ClanNameArgument(ClanManager clanManager) {
        super("Clan Name");
        this.clanManager = clanManager;
    }

    /**
     * Gets the native type that this argument uses,
     * the type that is sent to the client.
     *
     * @return native argument type
     */
    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    /**
     * Converts the value from the native type to the custom argument type.
     *
     * @param nativeType native argument provided value
     * @return converted value
     * @throws CommandSyntaxException if an exception occurs while parsing
     */
    @Override
    public @NotNull String convert(String nativeType) throws CommandSyntaxException {
        //todo split up error message
        if (!nativeType.matches("^[a-zA-Z0-9_]{" + clanManager.getMinCharactersInClanName() + "," + clanManager.getMaxCharactersInClanName() + "}$")) {
            throw ClanArgumentException.INVALID_CLAN_NAME.create(nativeType, clanManager.getMinCharactersInClanName(), clanManager.getMaxCharactersInClanName());
        }
        if (clanManager.getClanByName(nativeType).isPresent()) {
            throw ClanArgumentException.NAME_ALREADY_EXISTS.create(nativeType);
        }
        return nativeType;
    }
}
