package me.mykindos.betterpvp.core.command.brigadier.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;

@Singleton
public class OfflineClientArgumentType extends BPvPArgumentType<Client, String> implements CustomArgumentType.Converted<Client, String> {
    public static final DynamicCommandExceptionType UNKNOWNPLAYEREXCEPTION = new DynamicCommandExceptionType((name) -> new LiteralMessage("Unknown Player" + name));
    public static final DynamicCommandExceptionType INVALIDPLAYERNAMEEXCEPTION = new DynamicCommandExceptionType((name) -> new LiteralMessage("Invalid Playername " + name));
    private final ClientManager clientManager;
    @Inject
    protected OfflineClientArgumentType(ClientManager clientManager) {
        super("OfflineClient");
        this.clientManager = clientManager;
    }

    /**
     * Converts the value from the native type to the custom argument type.
     *
     * @param nativeType native argument provided value
     * @return converted value
     * @throws CommandSyntaxException if an exception occurs while parsing
     */
    @Override
    public Client convert(String nativeType) throws CommandSyntaxException {
        if (!nativeType.matches("^[a-zA-Z0-9_]{0,16}$")) {
            throw INVALIDPLAYERNAMEEXCEPTION.create(nativeType);
        }
        //TODO figure out how to do async/futures properly
        return clientManager.search().offline(nativeType).join().orElseThrow(() -> UNKNOWNPLAYEREXCEPTION.create(nativeType));
    }

    /**
     * Gets the native type that this argument uses,
     * the type that is sent to the client.
     *
     * @return native argument type
     */
    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}
