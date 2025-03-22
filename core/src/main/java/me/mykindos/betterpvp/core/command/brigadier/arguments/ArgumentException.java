package me.mykindos.betterpvp.core.command.brigadier.arguments;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ArgumentException {
    public static final SimpleCommandExceptionType INSUFFICIENT_PERMISSION = new SimpleCommandExceptionType(
            new LiteralMessage("Insufficient permissions to use this command")
    );
    public static final Dynamic3CommandExceptionType TARGET_ALREADY_INVITED_BY_ORIGIN_TYPE = new Dynamic3CommandExceptionType(
            (originName, targetName, type) -> new LiteralMessage(targetName + " already has a " + type + " invite with " + originName)
    );
    public static final Dynamic3CommandExceptionType TARGET_NOT_INVITED_BY_ORIGIN_TYPE = new Dynamic3CommandExceptionType(
            (originName, targetName, type) -> new LiteralMessage(targetName + " does not have a " + type + " invite with " + originName)
    );

    public static final DynamicCommandExceptionType TARGET_MUST_BE_PLAYER = new DynamicCommandExceptionType(
            (targetName) -> new LiteralMessage(targetName + " is not a player")
    );

    public static final DynamicCommandExceptionType COMMAND_ON_COOLDOWN = new DynamicCommandExceptionType(
            //time is in seconds
            (time) -> new LiteralMessage("You may use this command in " + time + " seconds")
    );
    public static final DynamicCommandExceptionType UNKNOWN_PLAYER = new DynamicCommandExceptionType(
            (playerName) -> new LiteralMessage(playerName + " does not match an online player")
    );
}
