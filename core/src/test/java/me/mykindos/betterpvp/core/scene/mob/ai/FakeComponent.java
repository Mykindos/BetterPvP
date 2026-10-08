package me.mykindos.betterpvp.core.scene.mob.ai;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** A component that records its lifecycle into a shared log as "start name", "tick name" and "stop name". */
public class FakeComponent implements AIComponent {

    private final String name;
    private final EnumSet<AIControl> controls;
    private final List<String> log;
    public boolean canStart = true;
    /** {@code null} leaves {@link #shouldContinue()} to the interface default. */
    public Boolean shouldContinue;
    public int starts;
    public int ticks;
    public int stops;

    public FakeComponent(String name, List<String> log, AIControl... controls) {
        this.name = name;
        this.log = log;
        this.controls = controls.length == 0 ? EnumSet.noneOf(AIControl.class) : EnumSet.of(controls[0], controls);
    }

    public FakeComponent(String name, AIControl... controls) {
        this(name, new ArrayList<>(), controls);
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return controls;
    }

    @Override
    public boolean canStart() {
        return canStart;
    }

    @Override
    public boolean shouldContinue() {
        return shouldContinue == null ? AIComponent.super.shouldContinue() : shouldContinue;
    }

    @Override
    public void start() {
        starts++;
        log.add("start " + name);
    }

    @Override
    public void tick() {
        ticks++;
        log.add("tick " + name);
    }

    @Override
    public void stop() {
        stops++;
        log.add("stop " + name);
    }

    public boolean isRunning() {
        return starts > stops;
    }
}
