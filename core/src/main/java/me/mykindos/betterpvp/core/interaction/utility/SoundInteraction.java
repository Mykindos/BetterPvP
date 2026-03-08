package me.mykindos.betterpvp.core.interaction.utility;

import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A utility interaction that plays sounds when executed.
 */
public class SoundInteraction extends AbstractInteraction {

    private final List<SoundEffect> sounds;

    private SoundInteraction(List<SoundEffect> sounds) {
        super("sound");
        this.sounds = sounds;
    }

    /**
     * Create a new SoundInteraction that plays the given sounds.
     *
     * @param sounds the sounds to play
     * @return a new SoundInteraction
     */
    public static SoundInteraction of(SoundEffect... sounds) {
        return new SoundInteraction(List.of(sounds));
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor,
                                                    @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance,
                                                    @Nullable ItemStack itemStack) {
        sounds.forEach(sound -> sound.play(actor.getLocation()));
        return InteractionResult.Success.ADVANCE;
    }
}
