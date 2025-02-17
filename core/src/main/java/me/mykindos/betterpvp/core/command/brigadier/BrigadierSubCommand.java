package me.mykindos.betterpvp.core.command.brigadier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BrigadierSubCommand {

    Class<? extends IBrigadierCommand> value();
}
