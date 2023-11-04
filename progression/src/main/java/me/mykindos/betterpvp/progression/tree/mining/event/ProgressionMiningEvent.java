package me.mykindos.betterpvp.progression.tree.mining.event;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.function.LongUnaryOperator;

@Getter
@Setter
public class ProgressionMiningEvent extends CustomEvent {

    Player player;

    Block block;

    LongUnaryOperator experienceModifier;

    public ProgressionMiningEvent(Player player, Block block, LongUnaryOperator experienceModifier) {
        this.player = player;
        this.block = block;
        this.experienceModifier = experienceModifier;
    }
}