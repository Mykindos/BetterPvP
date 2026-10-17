package me.mykindos.betterpvp.core.command.brigadier.arguments;

import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static me.mykindos.betterpvp.core.command.brigadier.arguments.CommandMessages.translatable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ArgumentException {
    public static final SimpleCommandExceptionType INSUFFICIENT_PERMISSION = new SimpleCommandExceptionType(
            translatable("core.command.error.insufficient_permission")
    );
    public static final Dynamic3CommandExceptionType TARGET_ALREADY_INVITED_BY_ORIGIN_TYPE = new Dynamic3CommandExceptionType(
            (originName, targetName, type) -> translatable("core.command.error.already_invited", originName, targetName, type)
    );
    public static final Dynamic3CommandExceptionType TARGET_NOT_INVITED_BY_ORIGIN_TYPE = new Dynamic3CommandExceptionType(
            (originName, targetName, type) -> translatable("core.command.error.not_invited", originName, targetName, type)
    );

    public static final SimpleCommandExceptionType PLAYER_ONLY = new SimpleCommandExceptionType(
            translatable("core.command.error.player_only")
    );

    public static final DynamicCommandExceptionType COMMAND_ON_COOLDOWN = new DynamicCommandExceptionType(
            //time is in seconds
            time -> translatable("core.command.error.on_cooldown", time)
    );
    public static final DynamicCommandExceptionType UNKNOWN_PLAYER = new DynamicCommandExceptionType(
            playerName -> translatable("core.command.error.unknown_player", playerName)
    );
    public static final DynamicCommandExceptionType UNKNOWN_EFFECT = new DynamicCommandExceptionType(
            name -> translatable("core.command.error.unknown_effect", name)
    );
    public static final DynamicCommandExceptionType UNKNOWN_UUIDITEM = new DynamicCommandExceptionType(
            uuid -> translatable("core.command.error.unknown_uuid_item", uuid)
    );
    public static final DynamicCommandExceptionType UNKNOWN_BPVPITEM = new DynamicCommandExceptionType(
            name -> translatable("core.command.error.unknown_item", name)
    );
    public static final DynamicCommandExceptionType INVALID_BOOLEAN = new DynamicCommandExceptionType(
            input -> translatable("core.command.error.invalid_boolean", input)
    );
    public static final DynamicCommandExceptionType INVALID_DURATION = new DynamicCommandExceptionType(
            input -> translatable("core.command.error.invalid_duration", input)
    );
}
