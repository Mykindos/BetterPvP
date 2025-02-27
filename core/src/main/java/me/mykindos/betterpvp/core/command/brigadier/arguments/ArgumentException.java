package me.mykindos.betterpvp.core.command.brigadier.arguments;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;

public class ArgumentException {
    public static final Dynamic3CommandExceptionType TARGET_ALREADY_INVITED_BY_ORIGIN_TYPE = new Dynamic3CommandExceptionType(
            (originName, targetName, type) -> new LiteralMessage(targetName + " already has a " + type + " invite with " + originName)
    );
    public static final Dynamic3CommandExceptionType TARGET_NOT_INVITED_BY_ORIGIN_TYPE = new Dynamic3CommandExceptionType(
            (originName, targetName, type) -> new LiteralMessage(targetName + " does not have a " + type + " invite with " + originName)
    );
}
