package me.mykindos.betterpvp.core.scene.display;

import me.mykindos.betterpvp.core.scene.SceneObject;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * A {@link SceneObject} wrapping a {@link TextDisplay} entity.
 * <p>
 * Configuration (text, scale, billboard mode) is supplied at construction time and applied
 * in {@link #onInit()} when the backing entity is bound. Text can be updated at any time
 * via {@link #updateText(Component)}.
 * <p>
 * Typical usage inside a {@link me.mykindos.betterpvp.core.scene.loader.SceneObjectLoader}:
 * <pre>
 *   TextDisplay entity = location.getWorld().spawn(location, TextDisplay.class);
 *   spawn(new SceneTextDisplay(text, scale, Display.Billboard.FIXED), entity);
 * </pre>
 */
public class SceneTextDisplay extends SceneObject {

    private Component text;
    private final float scale;
    private final Display.Billboard billboard;
    private final boolean shadowed;

    /**
     * @param text      the text to display
     * @param scale     uniform scale applied to the display entity
     * @param billboard how the display orients itself toward viewers
     */
    public SceneTextDisplay(Component text, float scale, Display.Billboard billboard) {
        this(text, scale, billboard, true);
    }

    /**
     * @param text      the text to display
     * @param scale     uniform scale applied to the display entity
     * @param billboard how the display orients itself toward viewers
     * @param shadowed  whether the text renders with a drop shadow
     */
    public SceneTextDisplay(Component text, float scale, Display.Billboard billboard, boolean shadowed) {
        this.text = text;
        this.scale = scale;
        this.billboard = billboard;
        this.shadowed = shadowed;
    }

    @Override
    protected void onInit() {
        final TextDisplay display = getTextEntity();
        display.setBackgroundColor(Color.fromARGB(0, 1, 1, 1));
        display.setShadowed(shadowed);
        display.setSeeThrough(false);
        display.setBillboard(billboard);
        display.setPersistent(false);
        display.setTransformation(new Transformation(
                new Vector3f(), new AxisAngle4f(), new Vector3f(scale), new AxisAngle4f()
        ));
        display.text(text);
    }

    /**
     * Returns the underlying {@link TextDisplay} entity.
     *
     * @throws IllegalStateException if {@link #init(org.bukkit.entity.Entity)} has not been called yet
     */
    public TextDisplay getTextEntity() {
        return (TextDisplay) getEntity();
    }

    /**
     * Updates the displayed text. Safe to call before or after {@link #init(org.bukkit.entity.Entity)};
     * if called before init the text is stored and applied in {@link #onInit()}.
     */
    public void updateText(Component newText) {
        this.text = newText;
        if (isInitialized()) {
            getTextEntity().text(newText);
        }
    }
}
