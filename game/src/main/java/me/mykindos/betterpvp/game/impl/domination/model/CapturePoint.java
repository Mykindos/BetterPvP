package me.mykindos.betterpvp.game.impl.domination.model;

import dev.brauw.mapper.region.CuboidRegion;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapNativeStat;
import me.mykindos.betterpvp.game.framework.model.Lifecycled;
import me.mykindos.betterpvp.game.framework.model.stats.StatManager;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.impl.domination.Domination;
import me.mykindos.betterpvp.game.impl.domination.controller.GameController;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.Map;
import java.util.WeakHashMap;

@Getter
public class CapturePoint implements Lifecycled {

    private final CuboidRegion region;
    private final BoundingBox captureArea;
    private final String name;
    private final GameController controller;
    private final DominationConfiguration configuration;
    private final StatManager statManager;

    /**
     * captureProgress is in the range [0.0, 1.0].
     * When the point is neutral (owningTeam == null), progress increases from 0.0 to 1.0 to capture the point.
     * When the point is captured (owningTeam != null), it starts at 1.0.
     * If an opposing team contests, the progress decays from 1.0 toward 0.0 at the same rate as neutral capture.
     * When no team is present, the progress is updated as follows:
     *   - If the point was previously captured and progress is above 0, it gradually recovers (increases)
     *     toward 1.0 at the same rate as it decays.
     *   - If the progress has fully decayed to 0, the point is considered neutral.
     */
    private Team owningTeam;
    private Team capturingTeam; // The team actively capturing (must differ from owningTeam)
    private double captureProgress = 0.0;
    private final CapturePointBlocks blocks;
    private final CapturePointFX fx;
    private State state;

    // Map of players on the point
    private final Map<Player, Team> playersOnPoint = new WeakHashMap<>();

    public enum State {
        NEUTRAL,     // No team owns the point (captureProgress = 0.0 when idle)
        CAPTURING,   // Actively being captured
        CAPTURED,    // Fully captured (progress at 1.0)
        REVERTING    // Progress is returning toward a stable state when no team is present
    }

    public CapturePoint(String name, CuboidRegion region, GameController controller, DominationConfiguration configuration, StatManager statManager,
                        ClientManager clientManager, Domination game) {
        this.name = name;
        this.region = region;
        this.configuration = configuration;
        this.statManager = statManager;
        final Location min = region.getMin();
        final Location max = region.getMax();
        this.captureArea = new BoundingBox(min.x(), min.y(), min.z(), max.x(), max.y(), max.z()).expand(-0.5, 0, -0.5);
        this.controller = controller;
        this.state = State.NEUTRAL;
        this.blocks = new CapturePointBlocks(this);
        this.fx = new CapturePointFX(this, clientManager, game);
    }

    @Override
    public void setup() {
        blocks.setup();
    }

    @Override
    public void tearDown() {
        blocks.tearDown();
    }

    public void tick() {
        // Remove players who are no longer in range or connected.
        playersOnPoint.keySet().removeIf(player -> !isInRange(player.getLocation()) || !player.isConnected());

        // Determine how many distinct teams are on the point.
        long distinctTeamCount = playersOnPoint.values().stream().distinct().count();

        // If multiple teams are present, freeze progress.
        if (distinctTeamCount > 1) {
            state = State.CAPTURING; // Freeze progress changes.
            final GameTeamMapNativeStat.GameTeamMapNativeStatBuilder<?, ?> builder =  GameTeamMapNativeStat.builder()
                    .action(GameTeamMapNativeStat.Action.CONTROL_POINT_TIME_CONTESTED);

            playersOnPoint.keySet().stream()
                    .map(Player::getUniqueId)
                    .forEach(id -> {
                        statManager.incrementGameMapStat(id, builder, 50);
                    });
        } else if (distinctTeamCount == 1) {
            // Consider one active team if exactly one team is present.
            Team activeTeam = playersOnPoint.values().stream().findAny().orElse(null);
            handleSingleTeamOnPoint(activeTeam);
        } else {
            // No team on point: handle decay/return.
            handleNoTeamsOnPoint();
        }

        blocks.tick();
        fx.tick();

        // Award points if the point is fully captured.
        if (owningTeam != null && Bukkit.getCurrentTick() % 10 == 0) {
            controller.addPoints(owningTeam, (int) Math.ceil(configuration.getCapturePointScoreAttribute().getValue() / 2d)); // 2 because it's every 10 ticks out of 20
        }
    }

    /**
     * Handles capturing logic when exactly one team is on the point.
     */
    private void handleSingleTeamOnPoint(Team activeTeam) {
        // If a capturing process is underway by a different team, ignore the new team and decay.
        if (capturingTeam != null && !capturingTeam.equals(activeTeam)) {
            handleNoTeamsOnPoint();
            return;
        }
        if (capturingTeam == null) {
            capturingTeam = activeTeam;
        }
        // The capture rate per tick (assuming 20 ticks per second).
        double progressPerTick = 1.0 / (configuration.getSecondsToCaptureAttribute().getValue() * 20) * playersOnPoint.size();

        if (owningTeam == null) {
            // Neutral capture: progress increases from 0.0 to 1.0.
            captureProgress += progressPerTick;
            //this stat must be incremented both if captured or are capturing, otherwise it loses 1 tick of progress
            final GameTeamMapNativeStat.GameTeamMapNativeStatBuilder<?, ?> timeBuilder =  GameTeamMapNativeStat.builder()
                    .action(GameTeamMapNativeStat.Action.CONTROL_POINT_TIME_CAPTURING);
            playersOnPoint.keySet().stream()
                    .map(Player::getUniqueId)
                    .forEach(id -> {
                        statManager.incrementGameMapStat(id, timeBuilder, 50);
                    });

            if (captureProgress >= 1.0) {
                captureProgress = 1.0;
                state = State.CAPTURED;
                owningTeam = activeTeam;
                capturingTeam = null;
                blocks.capture(owningTeam);
                fx.capture(owningTeam);
                final GameTeamMapNativeStat.GameTeamMapNativeStatBuilder<?, ?> builder =  GameTeamMapNativeStat.builder()
                        .action(GameTeamMapNativeStat.Action.CONTROL_POINT_CAPTURED);

                playersOnPoint.keySet().stream()
                        .map(Player::getUniqueId)
                        .forEach(id -> {
                            statManager.incrementGameMapStat(id, builder, 1);
                        });
                return;
            }
            state = State.CAPTURING;
        } else {
            // Contested capture: point is already captured.
            if (activeTeam.equals(owningTeam)) {
                // If the owner is present, maintain full capture.
                captureProgress = 1.0;
                state = State.CAPTURED;
                capturingTeam = null;
            } else {
                // Opposing team is contesting: decay progress from 1.0 toward 0.0.
                captureProgress -= progressPerTick;
                if (captureProgress <= 0.0) {
                    captureProgress = 0.0;
                    // Point is now neutral.
                    owningTeam = null;
                    blocks.uncapture();
                }
                state = State.CAPTURING;
                final GameTeamMapNativeStat.GameTeamMapNativeStatBuilder<?, ?> builder =  GameTeamMapNativeStat.builder()
                        .action(GameTeamMapNativeStat.Action.CONTROL_POINT_TIME_CAPTURING);

                playersOnPoint.keySet().stream()
                        .map(Player::getUniqueId)
                        .forEach(id -> {
                            statManager.incrementGameMapStat(id, builder, 50);
                        });
            }

        }
    }

    /**
     * Handles the decay (or return) of capturing progress when no team is on the point.
     * For a neutral point (owningTeam == null), progress decays toward 0 gradually.
     * For a point that was owned (owningTeam != null), progress recovers toward 1 gradually,
     * at the same rate as it decayed, unless it has already fully decayed to 0.
     */
    private void handleNoTeamsOnPoint() {
        double progressPerTick = 1.0 / (configuration.getSecondsToCaptureAttribute().getValue() * 20);

        if (owningTeam == null) {
            // For a neutral point, progress decays toward 0.
            if (captureProgress > 0.0) {
                captureProgress = Math.max(0.0, captureProgress - progressPerTick);
            } else {
                capturingTeam = null;
            }
            state = (captureProgress == 0.0) ? State.NEUTRAL : State.REVERTING;
        } else {
            // For a point that was owned, if progress is above 0, it gradually recovers.
            if (captureProgress < 1.0 && captureProgress > 0.0) {
                captureProgress = Math.min(1.0, captureProgress + progressPerTick);
                state = (captureProgress == 1.0) ? State.CAPTURED : State.REVERTING;
            }
            // If progress has decayed to 0, the point is neutral
            else if (captureProgress == 0.0) {
                state = State.NEUTRAL;
                owningTeam = null;
                capturingTeam = null;
                blocks.uncapture();
            }
        }
    }

    public boolean isInRange(Location playerLocation) {
        if (!playerLocation.getWorld().equals(region.getWorld())) {
            return false;
        }
        return region.contains(playerLocation);
    }

    public void addPlayer(Player player, Team team) {
        playersOnPoint.put(player, team);
    }
}
