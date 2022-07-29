package me.mykindos.betterpvp.core.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Command implements ICommand{

    private boolean enabled;

}
