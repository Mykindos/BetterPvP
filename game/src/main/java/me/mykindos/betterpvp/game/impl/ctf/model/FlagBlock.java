package me.mykindos.betterpvp.game.impl.ctf.model;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.utilities.model.ProgressColor;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

@RequiredArgsConstructor
public class FlagBlock {

    private final Flag flag;
    private BlockDisplay display;
    private TextDisplay textDisplay;
    private Location currentLocation;

    public void spawn(Location location) {
        this.currentLocation = location;

        if (display == null || !display.isValid()) {
            display = currentLocation.getWorld().spawn(currentLocation, BlockDisplay.class, spawned -> {
                spawned.setBlock(flag.getMaterial().createBlockData());
                spawned.setTransformation(new Transformation(
                        new Vector3f(flag.getSize() / -2, -0f, flag.getSize() / -2), // to center it
                        new AxisAngle4f(),
                        new Vector3f(flag.getSize()),
                        new AxisAngle4f()
                ));
                spawned.setShadowStrength(2.0f);
                spawned.setPersistent(false);
                spawned.setGlowColorOverride(flag.getTeam().getProperties().vanillaColor().getColor());
                spawned.setGlowing(true);
                spawned.teleport(currentLocation); // For rotation
            });
            ;
        } else {
            display.teleport(currentLocation);
        }

        final Location textLocation = currentLocation.clone().add(0, flag.getSize() * 2, 0);
        if (textDisplay == null || !textDisplay.isValid()) {
            textDisplay = currentLocation.getWorld().spawn(textLocation, TextDisplay.class, spawned -> {
                spawned.setTransformation(new Transformation(
                        new Vector3f(),
                        new AxisAngle4f(),
                        new Vector3f(flag.getSize()),
                        new AxisAngle4f()
                ));
                spawned.setBillboard(Display.Billboard.VERTICAL);
                spawned.setDefaultBackground(false);
                spawned.setBackgroundColor(Color.fromARGB(0x0));
                spawned.setShadowed(true);
                spawned.setPersistent(false);
            });
        } else {
            textDisplay.teleport(textLocation);
        }
    }

    public void tick() {
        if (textDisplay != null && textDisplay.isValid()) {
            final TeamProperties properties = flag.getTeam().getProperties();
            Component text = Component.text(properties.name() + " Flag", properties.color(), TextDecoration.BOLD);
            if (flag.getState() == Flag.State.DROPPED) {
                text = text.appendNewline().appendNewline().append(getFlagCountdown());
            }
            textDisplay.text(text);
        }
    }

    public Component getFlagCountdown() {
        final TextColor numberColor = ProgressColor.of((float) (flag.getReturnCountdown() / Flag.RETURN_COUNTDOWN)).getTextColor();
        final String remaining = String.format("%d", (int) flag.getReturnCountdown());
        return Component.text(remaining, numberColor);
    }

    public void pickup(Player player) {
        currentLocation = player.getLocation();
        if (display != null && display.isValid()) {
            display.remove();
        }
        if (textDisplay != null && textDisplay.isValid()) {
            textDisplay.remove();
        }
    }

    public void drop(Location location) {
        location = location.clone();
        location.setPitch(0);
        spawn(location);
    }

    public void returnToBase() {
        spawn(flag.getBaseLocation());
    }

    public void destroy() {
        display.remove();
        display = null;

        textDisplay.remove();
        textDisplay = null;
    }
}