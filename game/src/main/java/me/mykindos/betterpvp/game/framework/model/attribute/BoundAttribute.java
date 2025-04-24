package me.mykindos.betterpvp.game.framework.model.attribute;

/**
 * An attribute that is bound to a game instance.
 * @param <T> the type of the attribute
 */
public abstract class BoundAttribute<T> extends GameAttribute<T> {

    protected BoundAttribute(String key, T defaultValue) {
        super(key, defaultValue);
    }
}
