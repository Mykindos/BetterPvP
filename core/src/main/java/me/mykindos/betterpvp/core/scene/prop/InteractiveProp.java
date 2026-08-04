package me.mykindos.betterpvp.core.scene.prop;

import lombok.Setter;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A {@link SimpleProp} that answers a right-click - a ship's helm, a quest board, a lever that starts something.
 * <p>
 * Being an {@link Actor} is not free: {@code SceneInteractListener} suppresses block placement and item use for anyone
 * merely <em>looking</em> at one, so that clicking a prop never also swings the held item. That is right for something
 * meant to be clicked and wrong for scenery, which is why decorative props stay a plain {@link SimpleProp}.
 * <p>
 * The handler is set by a decorator, so it is re-attached every time the prop re-materializes after a chunk cycle.
 */
public class InteractiveProp extends SimpleProp implements Actor {

    @Setter
    @Nullable
    private Consumer<Player> interactionHandler;

    public InteractiveProp(SceneObjectFactory factory) {
        super(factory);
    }

    @Override
    public void act(Player runner) {
        if (interactionHandler != null) {
            interactionHandler.accept(runner);
        }
    }
}
