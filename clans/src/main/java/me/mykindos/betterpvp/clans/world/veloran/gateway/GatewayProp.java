package me.mykindos.betterpvp.clans.world.veloran.gateway;

import dev.brauw.mapper.region.CuboidRegion;
import lombok.Getter;
import me.mykindos.betterpvp.core.scene.prop.Prop;
import me.mykindos.betterpvp.core.scene.prop.PropFactory;
import me.mykindos.betterpvp.core.world.zone.RegionBounds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * The visible portal of The Sundered Gate: a {@link Prop} whose backing entity is a floating {@link TextDisplay} label
 * and which owns the {@link CuboidRegion} that defines the portal volume.
 * <p>
 * On {@link #onInit()} it styles the label and attaches the two behaviours that bring the portal to life:
 * {@link GatewayParticleBehavior} (ambiance) and {@link GatewayTeleportBehavior} (the entry trigger). The cuboid is
 * exposed via {@link #getPortalMarker()} so those behaviours hit-test against the exact same volume the gate zone protects.
 */
@Getter
public class GatewayProp extends Prop {

    private final Location portalMarker;
    private final CuboidRegion portalRegion;
    private final Component label;

    public GatewayProp(PropFactory factory, Location portalMarker, CuboidRegion portalRegion, Component label) {
        super(factory);
        this.portalMarker = portalMarker;
        this.portalRegion = portalRegion;
        this.label = label;
    }

    @Override
    protected void onInit() {
        final TextDisplay display = (TextDisplay) getEntity();
        display.text(Component.empty()
                .append(label)
                .appendNewline()
                .append(Component.text("Step into the unknown...", NamedTextColor.GRAY, TextDecoration.ITALIC)));
        display.setBillboard(Display.Billboard.CENTER);
        display.setShadowed(true);
        display.setSeeThrough(false);
        display.setBrightness(new Display.Brightness(5, 5));
        display.setViewRange(500);
        display.setPersistent(false);
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        display.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f(),
                new Vector3f(4.5f),
                new AxisAngle4f()
        ));

        addBehavior(new GatewayParticleBehavior(portalRegion, portalMarker.getDirection()));
        addBehavior(new GatewayAmbientSoundBehavior(portalRegion, 1.0f));
        addBehavior(new GatewayTeleportBehavior(portalRegion));
    }
}
