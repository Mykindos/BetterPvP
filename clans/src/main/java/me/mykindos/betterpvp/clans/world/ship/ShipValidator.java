package me.mykindos.betterpvp.clans.world.ship;

import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import dev.brauw.mapper.validation.ValidationIssue;
import me.mykindos.betterpvp.clans.world.content.DataPointValidator;
import me.mykindos.betterpvp.core.world.site.ArrivalPoints;
import me.mykindos.betterpvp.core.world.schematic.StructureAnchor;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Checks ship data-points while the builder is still standing next to them.
 * <p>
 * Covers both halves of the system, because a world is usually one or the other: a dock world holds berths and
 * navigators, and the world a vessel is authored in holds the markers that go inside its structure.
 * <p>
 * Every rule here corresponds to a failure that is otherwise silent or arrives hours later in a log — an anchor
 * authored as a point pastes every copy of the ship unrotated, and a navigator naming a berth that does not exist only
 * says so when a player clicks it.
 *
 * @see ShipService for what each data-point means
 */
public class ShipValidator extends DataPointValidator {

    @Override
    public String name() {
        return "ships";
    }

    @Override
    public List<ValidationIssue> validate(World world, List<Region> regions) {
        final List<ValidationIssue> issues = new ArrayList<>();

        final List<Region> berths = named(regions, ShipService.BERTH_POINT);
        final List<Region> navigators = named(regions, ShipService.NAVIGATOR_POINT);
        final List<Region> anchors = named(regions, StructureAnchor.POINT);
        final List<Region> boards = named(regions, ShipService.BOARD_POINT);
        final List<Region> helms = named(regions, ShipHelms.HELM_POINT);
        final List<Region> arrivals = named(regions, ArrivalPoints.DEFAULT_MARKER);

        requireType(issues, berths, PerspectiveRegion.class, "a perspective marker (its facing turns the hull)");
        requireType(issues, navigators, PointRegion.class, "a point");
        requireType(issues, anchors, PerspectiveRegion.class,
                "a perspective marker (rotation is measured from its facing)");
        requireType(issues, boards, PerspectiveRegion.class, "a perspective marker (it decides which way boarders face)");
        requireType(issues, helms, PerspectiveRegion.class,
                "a perspective marker (its facing mounts the wheel on the wall)");
        requireType(issues, arrivals, PerspectiveRegion.class, "a perspective marker (it decides which way arrivals face)");

        final Map<String, Region> berthIds = collectIds(issues, berths, "berth");
        validateNavigators(issues, navigators, berthIds);
        validateVesselMarkers(issues, anchors, boards, helms);
        return issues;
    }

    /**
     * A navigator whose berth does not exist reads in-game as a broken NPC: it takes the click and refuses, with
     * nothing to say which of the two is wrong.
     */
    private void validateNavigators(List<ValidationIssue> issues, List<Region> navigators, Map<String, Region> berthIds) {
        for (Region navigator : navigators) {
            final String berthId = tags(navigator).getString("ship", "").trim();
            if (berthId.isEmpty()) {
                issues.add(ValidationIssue.error(navigator,
                        "Has no 'ship' tag, so it has no ship to put anyone on"));
            } else if (!berthIds.containsKey(berthId.toLowerCase(Locale.ROOT))) {
                issues.add(ValidationIssue.error(navigator,
                        "Boards ship '" + berthId + "', which no " + ShipService.BERTH_POINT + " declares"));
            }
        }
    }

    /**
     * The markers that travel inside a vessel's structure. Only checked once one of them is present, so a plain dock
     * world is not told it is missing markers it was never meant to have.
     */
    private void validateVesselMarkers(List<ValidationIssue> issues, List<Region> anchors, List<Region> boards,
                                       List<Region> helms) {
        if (anchors.isEmpty() && boards.isEmpty() && helms.isEmpty()) {
            return;
        }

        if (anchors.isEmpty()) {
            // Without an anchor there is no origin to measure from and nothing carrying the vessel's capacity, and the
            // marker the rest of the ship is authored around is the one thing a builder cannot leave out.
            boards.forEach(board -> issues.add(ValidationIssue.warning(board,
                    "This vessel has no " + StructureAnchor.POINT
                            + " - it will be anchored wherever the builder stands when captured")));
        }
        if (boards.isEmpty()) {
            anchors.forEach(anchor -> issues.add(ValidationIssue.warning(anchor,
                    "This vessel has no " + ShipService.BOARD_POINT
                            + " - boarders will land on the berth marker, at the waterline")));
        }
        if (helms.isEmpty()) {
            anchors.forEach(anchor -> issues.add(ValidationIssue.warning(anchor,
                    "This vessel has no " + ShipHelms.HELM_POINT + " - its captain will have no way to set a course")));
        }

        // One vessel per structure: a second of any of these is picked between by file order, which is not a choice
        // anybody made.
        if (anchors.size() > 1) {
            issues.add(ValidationIssue.error(anchors.getLast(), "A structure can only have one " + StructureAnchor.POINT));
        }
        if (boards.size() > 1) {
            issues.add(ValidationIssue.error(boards.getLast(), "A vessel can only have one " + ShipService.BOARD_POINT));
        }
        if (helms.size() > 1) {
            issues.add(ValidationIssue.error(helms.getLast(), "A vessel can only have one " + ShipHelms.HELM_POINT));
        }
    }
}
