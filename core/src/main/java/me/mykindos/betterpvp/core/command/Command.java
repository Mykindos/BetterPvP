package me.mykindos.betterpvp.core.command;

import lombok.Setter;

import java.util.List;
import java.util.ArrayList;

public abstract class Command implements ICommand {

    @Setter
    private boolean enabled;

    protected List<String> aliases;
    protected List<ISubCommand> subCommands;

    public Command() {
        aliases = new ArrayList<>();
        subCommands = new ArrayList<>();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public List<ISubCommand> getSubCommands(){
        return subCommands;
    }

}
