package me.mykindos.betterpvp.core.command;

import lombok.Setter;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;


public abstract class SubCommand implements ICommand {

    protected List<String> aliases;

    public SubCommand(){
        aliases = new ArrayList<>();
    }

    @Setter
    private boolean enabled;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();

    }

}