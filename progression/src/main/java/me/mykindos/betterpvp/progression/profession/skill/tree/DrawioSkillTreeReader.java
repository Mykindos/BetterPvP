package me.mykindos.betterpvp.progression.profession.skill.tree;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.utilities.DrawioDocumentReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Reads a draw.io file and maps skill nodes plus orthogonal edge geometry into
 * inventory skill-tree cells.
 */
@CustomLog
public class DrawioSkillTreeReader implements SkillTreeReader {

    private static final int SLOT_PX = 60;

    @Override
    public SkillTreeLayout read(File file) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            return read(in);
        }
    }

    private SkillTreeLayout read(InputStream in) throws Exception {
        Document doc = DrawioDocumentReader.parse(in);
        doc.getDocumentElement().normalize();

        Map<String, PositionedSkillNode> nodesById = new LinkedHashMap<>();

        collectWrappedProgressionNodes(doc.getElementsByTagName("UserObject"), nodesById);
        collectWrappedProgressionNodes(doc.getElementsByTagName("object"), nodesById);

        NodeList allCells = doc.getElementsByTagName("mxCell");
        List<Element> edgeCells = new ArrayList<>();

        for (int i = 0; i < allCells.getLength(); i++) {
            Element cell = (Element) allCells.item(i);
            if ("1".equals(cell.getAttribute("edge")) && cell.hasAttribute("source") && cell.hasAttribute("target")) {
                edgeCells.add(cell);
                continue;
            }
            if ("1".equals(cell.getAttribute("vertex"))) {
                String nodeId = DrawioDocumentReader.drawioId(cell);
                String value = cell.getAttribute("value");
                if (nodeId.isBlank()) {
                    nodeId = value;
                }
                String drawioId = cell.getAttribute("id");
                if (drawioId.isBlank()) {
                    drawioId = nodeId;
                }
                if (!nodeId.isBlank() && !nodesById.containsKey(drawioId) && !nodeId.equals("0") && !nodeId.equals("1")) {
                    PositionedSkillNode node = toNode(nodeId, cell);
                    if (node != null) {
                        nodesById.put(drawioId, node);
                        if (!nodeId.equals(drawioId)) {
                            nodesById.putIfAbsent(nodeId, node);
                        }
                    }
                }
            }
        }

        Map<Integer, SkillTreeCell> cells = new LinkedHashMap<>();
        int maxRow = 0;

        for (PositionedSkillNode node : new LinkedHashSet<>(nodesById.values())) {
            cells.put(SkillTreeLayout.slotIndex(node.row(), node.slotX()), new SkillTreeCell.Skill(node.skillId()));
            maxRow = Math.max(maxRow, node.row());
        }

        for (Element edge : edgeCells) {
            PositionedSkillNode source = nodesById.get(edge.getAttribute("source"));
            PositionedSkillNode target = nodesById.get(edge.getAttribute("target"));
            if (source == null || target == null) continue;

            List<int[]> waypoints = extractWaypoints(edge);
            if (waypoints.isEmpty()) {
                waypoints = defaultWaypoints(source, target);
            }

            fillConnectionCells(source, target, waypoints, cells);
        }

        log.info("Loaded draw.io skill tree: {} skill nodes, {} edges -> {} occupied cells",
                new LinkedHashSet<>(nodesById.values()).size(), edgeCells.size(), cells.size()).submit();

        return new SkillTreeLayout(maxRow + 1, cells);
    }

    private void fillConnectionCells(
            PositionedSkillNode source, PositionedSkillNode target,
            List<int[]> waypoints, Map<Integer, SkillTreeCell> cells
    ) {
        List<int[]> path = new ArrayList<>();
        path.add(new int[]{source.slotX(), source.row()});
        path.addAll(waypoints);
        path.add(new int[]{target.slotX(), target.row()});

        List<String> linked = List.of(source.skillId(), target.skillId());

        for (int seg = 0; seg < path.size() - 1; seg++) {
            int[] a = path.get(seg);
            int[] b = path.get(seg + 1);
            int dx = Integer.signum(b[0] - a[0]);
            int dy = Integer.signum(b[1] - a[1]);
            if (dx == 0 && dy == 0) continue;

            int x = a[0], y = a[1];
            while (x != b[0] || y != b[1]) {
                x += dx;
                y += dy;

                if (x == source.slotX() && y == source.row()) continue;
                if (x == target.slotX() && y == target.row()) continue;

                boolean isBend = (x == b[0] && y == b[1]) && seg < path.size() - 2;
                ConnectionType type;
                if (isBend) {
                    int[] c = path.get(seg + 2);
                    int nextDx = Integer.signum(c[0] - b[0]);
                    int nextDy = Integer.signum(c[1] - b[1]);
                    type = cornerType(dx, dy, nextDx, nextDy);
                } else {
                    type = dy == 0 ? ConnectionType.STRAIGHT_HORIZONTAL : ConnectionType.STRAIGHT_VERTICAL;
                }

                cells.put(SkillTreeLayout.slotIndex(y, x), new SkillTreeCell.Connection(type, linked));
            }
        }
    }

    private List<int[]> defaultWaypoints(PositionedSkillNode source, PositionedSkillNode target) {
        if (source.slotX() == target.slotX()) return List.of();
        // draw.io orthogonal routing places the horizontal segment at the midpoint row
        int midRow = (source.row() + target.row()) / 2;
        return List.of(
                new int[]{source.slotX(), midRow},
                new int[]{target.slotX(), midRow}
        );
    }

    private ConnectionType cornerType(int inDx, int inDy, int outDx, int outDy) {
        if (inDy != 0) {
            if (inDy == -1) return outDx == 1 ? ConnectionType.UP_RIGHT : ConnectionType.UP_LEFT;
            return outDx == 1 ? ConnectionType.DOWN_RIGHT : ConnectionType.DOWN_LEFT;
        } else {
            if (outDy == -1) return inDx == 1 ? ConnectionType.DOWN_LEFT : ConnectionType.DOWN_RIGHT;
            return inDx == 1 ? ConnectionType.UP_LEFT : ConnectionType.UP_RIGHT;
        }
    }

    private void collectWrappedProgressionNodes(NodeList wrappers, Map<String, PositionedSkillNode> nodesById) {
        for (int i = 0; i < wrappers.getLength(); i++) {
            Element wrapper = (Element) wrappers.item(i);
            String nodeId = DrawioDocumentReader.drawioId(wrapper);
            if (nodeId.isBlank()) continue;

            String drawioId = wrapper.getAttribute("id");
            if (drawioId.isBlank()) {
                drawioId = nodeId;
            }
            if (nodesById.containsKey(drawioId) || nodesById.containsKey(nodeId)) continue;

            NodeList mxCells = wrapper.getElementsByTagName("mxCell");
            if (mxCells.getLength() == 0) continue;

            Element mxCell = (Element) mxCells.item(0);
            if (!"1".equals(mxCell.getAttribute("vertex"))) continue;

            PositionedSkillNode node = toNode(nodeId, mxCell);
            if (node != null) {
                nodesById.put(drawioId, node);
                if (!nodeId.equals(drawioId)) {
                    nodesById.putIfAbsent(nodeId, node);
                }
            }
        }
    }

    private PositionedSkillNode toNode(String skillId, Element mxCell) {
        NodeList geoList = mxCell.getElementsByTagName("mxGeometry");
        if (geoList.getLength() == 0) return null;
        Element geo = (Element) geoList.item(0);
        return new PositionedSkillNode(skillId, toSlot(geo.getAttribute("y")), toSlot(geo.getAttribute("x")));
    }

    private List<int[]> extractWaypoints(Element edgeCell) {
        List<int[]> result = new ArrayList<>();
        NodeList arrays = edgeCell.getElementsByTagName("Array");
        for (int i = 0; i < arrays.getLength(); i++) {
            Element arr = (Element) arrays.item(i);
            if (!"points".equals(arr.getAttribute("as"))) continue;
            NodeList points = arr.getElementsByTagName("mxPoint");
            for (int j = 0; j < points.getLength(); j++) {
                Element pt = (Element) points.item(j);
                result.add(new int[]{toSlot(pt.getAttribute("x")), toSlot(pt.getAttribute("y"))});
            }
        }
        return result;
    }

    private int toSlot(String pxValue) {
        if (pxValue == null || pxValue.isBlank()) return 0;
        return (int) (Double.parseDouble(pxValue) / SLOT_PX);
    }
}
