package me.mykindos.betterpvp.core.components.champions.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlayerUseSkillEvent extends CustomCancellableEvent {

    private final Player player;
    private final IChampionsSkill skill;
    private final int level;
    @Nullable
    private CompletableFuture<Boolean> activated = null;

}