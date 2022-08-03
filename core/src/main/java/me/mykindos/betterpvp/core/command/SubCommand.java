package me.mykindos.betterpvp.core.command;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Rank;

import java.util.ArrayList;
import java.util.List;


@Getter
public abstract class SubCommand implements ICommand {

    protected List<String> aliases;

    @Setter
    private Rank requiredRank;

    @Setter
    private boolean enabled;

    public SubCommand(){
        aliases = new ArrayList<>();
    }

    @Override
    public Rank getRequiredRank() {
        return requiredRank;
    }

}