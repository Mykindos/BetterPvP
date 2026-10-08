package me.mykindos.betterpvp.core.world.construction.view;

import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.interaction.HitboxVolume;
import me.mykindos.betterpvp.core.scene.prop.Prop;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The scene object that stands for a structure: a two-line label saying what it is and how it is coming along. While
 * the label asks for an action, it carries a hitbox around itself, and clicking it takes that action.
 * <p>
 * The label is its body, so it comes and goes with the chunk it floats in. The blocks of the building itself are not
 * part of it: those are drawn into the world whether anyone is near or not.
 */
public final class StructureProp extends Prop implements Actor {

    private final SceneObjectRegistry registry;
    private final BoundingBox area;
    private final Consumer<Player> onClaim;

    private Component label = Component.empty();
    private boolean claimable;
    private @Nullable HitboxVolume hitbox;

    StructureProp(@NotNull ConstructionPropFactory factory, @NotNull SceneObjectRegistry registry,
                  @NotNull Location at, @NotNull Consumer<Player> onClaim) {
        super(factory);
        this.registry = registry;
        this.area = new BoundingBox(at.getX() - 0.75, at.getY() - 0.25, at.getZ() - 0.75,
                at.getX() + 0.75, at.getY() + 0.75, at.getZ() + 0.75);
        this.onClaim = onClaim;
    }

    @Override
    protected void onInit() {
        final TextDisplay display = (TextDisplay) getEntity();
        display.setBillboard(Display.Billboard.CENTER);
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        display.setShadowed(true);
        display.setViewRange(0.75f);
        display.text(label);
        if (claimable) {
            addHitbox();
        }
    }

    @Override
    protected void onDematerialize() {
        hitbox = null;
        super.onDematerialize();
    }

    void setLabel(@NotNull Component label) {
        if (label.equals(this.label)) {
            return;
        }
        this.label = label;
        if (isMaterialized()) {
            ((TextDisplay) getEntity()).text(label);
        }
    }

    void setClaimable(boolean claimable) {
        if (this.claimable == claimable) {
            return;
        }
        this.claimable = claimable;
        if (!isMaterialized()) {
            return;
        }
        if (claimable) {
            addHitbox();
        } else if (hitbox != null) {
            removeBehavior(hitbox);
            hitbox = null;
        }
    }

    @NotNull Component getLabel() {
        return label;
    }

    /** The box around the label a click claims it through, or null while there is nothing to claim. */
    @Nullable BoundingBox clickArea() {
        return claimable ? area.clone() : null;
    }

    @Override
    public void act(Player runner) {
        if (claimable) {
            onClaim.accept(runner);
        }
    }

    private void addHitbox() {
        hitbox = new HitboxVolume(this, registry, area);
        addBehavior(hitbox);
    }
}
