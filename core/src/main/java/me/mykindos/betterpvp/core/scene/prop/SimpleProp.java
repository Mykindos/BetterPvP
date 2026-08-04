package me.mykindos.betterpvp.core.scene.prop;

import me.mykindos.betterpvp.core.scene.SceneObjectFactory;

/**
 * A prop with no behaviour of its own, assembled entirely from the outside.
 * <p>
 * Everything that makes one prop different from another - its model, its ambient sound, the particles it throws - is
 * attached by a {@link me.mykindos.betterpvp.core.scene.SceneObject#addDecorator decorator}, exactly as spawn's
 * residents are built from a plain {@code ModeledNPC}. So a lantern, a crate and a rune circle are three markers on a
 * map, not three classes.
 * <p>
 * Use {@link InteractiveProp} instead when clicking it should do something; this one is deliberately inert.
 */
public class SimpleProp extends ModeledProp {

    public SimpleProp(SceneObjectFactory factory) {
        super(factory);
    }
}
