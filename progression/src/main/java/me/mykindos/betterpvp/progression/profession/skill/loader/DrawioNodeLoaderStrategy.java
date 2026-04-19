package me.mykindos.betterpvp.progression.profession.skill.loader;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.utilities.DrawioDocumentReader;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionAttributeNode;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNode;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeDependency;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionRecipeNode;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;
import net.kyori.adventure.key.Key;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CustomLog
public class DrawioNodeLoaderStrategy implements NodeLoaderStrategy {

    private final Progression plugin;
    private final File drawioFile;

    public DrawioNodeLoaderStrategy(Progression plugin, File drawioFile) {
        this.plugin = plugin;
        this.drawioFile = drawioFile;
    }

    @Override
    public List<ProfessionNode> loadNodes(Progression progression, String profession) {
        if (!drawioFile.exists()) {
            log.warn("Draw.io node file not found: {}", drawioFile.getPath()).submit();
            return List.of();
        }

        List<ProfessionNode> nodes = new ArrayList<>();
        Map<String, ProfessionNode> nodesByDrawioId = new HashMap<>();

        try (InputStream in = new FileInputStream(drawioFile)) {
            Document doc = DrawioDocumentReader.parse(in);
            doc.getDocumentElement().normalize();

            collectNodes(doc.getElementsByTagName("UserObject"), progression, profession, nodes, nodesByDrawioId);
            collectNodes(doc.getElementsByTagName("object"), progression, profession, nodes, nodesByDrawioId);
            applyDirectedEdgeDependencies(doc, nodesByDrawioId);
        } catch (Exception e) {
            log.error("Failed to load nodes from draw.io file {}: {}", drawioFile.getPath(), e.getMessage()).submit();
        }

        return nodes;
    }

    private void collectNodes(
            NodeList wrappers,
            Progression progression,
            String profession,
            List<ProfessionNode> out,
            Map<String, ProfessionNode> nodesByDrawioId
    ) {
        for (int i = 0; i < wrappers.getLength(); i++) {
            Element wrapper = (Element) wrappers.item(i);
            String id = DrawioDocumentReader.drawioId(wrapper);
            String drawioId = wrapper.getAttribute("id");
            if (drawioId.isBlank()) {
                drawioId = id;
            }
            if (nodesByDrawioId.containsKey(drawioId) || nodesByDrawioId.containsKey(id)) continue;

            NodeList mxCells = wrapper.getElementsByTagName("mxCell");
            if (mxCells.getLength() == 0) continue;

            Element mxCell = (Element) mxCells.item(0);
            if (!"1".equals(mxCell.getAttribute("vertex"))) continue;

            String label = wrapper.getAttribute("label");
            String style = mxCell.getAttribute("style");

            ProfessionNode node;
            if (isRecipeNode(wrapper)) {
                if (id.isBlank() || id.equals("0") || id.equals("1")) continue;
                node = buildRecipeNode(id, wrapper);
            } else if (style.contains("ellipse")) {
                if (id.isBlank() || id.equals("0") || id.equals("1")) continue;
                node = buildAttributeNode(id, wrapper);
            } else {
                if (id.isBlank() || id.equals("0") || id.equals("1")) continue;
                node = buildSkillNode(id, wrapper);
            }

            if (node == null) continue;

            progression.getInjector().injectMembers(node);
            node.initialize(profession);
            if (!label.isBlank()) {
                // replace newline character and consecutive whitespaces, and new line
                label = label.replace("&#xa;", "\n")
                        .replace('\u00A0', ' ')
                        .replaceAll("\\s*\\R\\s*", " ")
                        .replaceAll(" +", " ")
                        .trim();
                node.setDisplayName(label);
            }

            out.add(node);
            nodesByDrawioId.put(drawioId, node);
            nodesByDrawioId.putIfAbsent(id, node);
        }
    }

    private void applyDirectedEdgeDependencies(Document doc, Map<String, ProfessionNode> nodesByDrawioId) {
        Map<ProfessionNode, Set<String>> parentIdsByChild = new HashMap<>();
        NodeList cells = doc.getElementsByTagName("mxCell");
        int directedEdges = 0;

        for (int i = 0; i < cells.getLength(); i++) {
            Element cell = (Element) cells.item(i);
            if (!"1".equals(cell.getAttribute("edge"))) continue;

            ProfessionNode source = nodesByDrawioId.get(cell.getAttribute("source"));
            ProfessionNode target = nodesByDrawioId.get(cell.getAttribute("target"));
            if (source == null || target == null) continue;

            EdgeDependency dependency = edgeDependency(cell, source, target);
            if (dependency == null) continue;

            directedEdges++;
            parentIdsByChild.computeIfAbsent(dependency.child(), ignored -> new LinkedHashSet<>())
                    .add(dependency.parent().getName());
        }

        for (ProfessionNode node : new LinkedHashSet<>(nodesByDrawioId.values())) {
            Set<String> parentIds = parentIdsByChild.getOrDefault(node, Set.of());
            int requiredLevel = node.getDependencies() == null ? 0 : node.getDependencies().getRequiredLevel();
            int levelsRequired = parentIds.isEmpty() ? 0 : parentIds.size();
            node.setDependencies(new ProfessionNodeDependency(new ArrayList<>(parentIds), levelsRequired, requiredLevel));
        }

        log.info("Loaded {} directed draw.io dependency edges from {}", directedEdges, drawioFile.getName()).submit();
    }

    private EdgeDependency edgeDependency(Element edge, ProfessionNode source, ProfessionNode target) {
        Map<String, String> style = parseStyle(edge.getAttribute("style"));
        boolean startArrow = hasArrow(style.get("startArrow"));
        boolean endArrow = hasArrow(style.get("endArrow"));
        boolean startArrowDisabled = isArrowDisabled(style.get("startArrow"));
        boolean endArrowDisabled = isArrowDisabled(style.get("endArrow"));

        if (startArrow && endArrow) {
            return null;
        }

        if (startArrow && endArrowDisabled) {
            return new EdgeDependency(target, source);
        }

        if (endArrowDisabled && (style.containsKey("startArrow") || !startArrow)) {
            return null;
        }

        // draw.io stores the direction in source/target even when the default end arrow is omitted.
        return new EdgeDependency(source, target);
    }

    private Map<String, String> parseStyle(String style) {
        Map<String, String> values = new HashMap<>();
        if (style == null || style.isBlank()) {
            return values;
        }

        for (String part : style.split(";")) {
            int equalsIndex = part.indexOf('=');
            if (equalsIndex <= 0) continue;
            values.put(part.substring(0, equalsIndex), part.substring(equalsIndex + 1));
        }

        return values;
    }

    private boolean hasArrow(String arrowStyle) {
        return arrowStyle != null && !arrowStyle.isBlank() && !"none".equalsIgnoreCase(arrowStyle);
    }

    private boolean isArrowDisabled(String arrowStyle) {
        return "none".equalsIgnoreCase(arrowStyle);
    }

    private ProfessionAttributeNode buildAttributeNode(String id, Element wrapper) {
        int maxLevel = parseInt(wrapper.getAttribute("max_level"), 1);
        Map<ProfessionAttribute, ProfessionAttributeNode.AttributeConfig> attributeMap = new HashMap<>();

        // Single attribute (no suffix)
        String attrName = wrapper.getAttribute("attribute");
        if (!attrName.isBlank()) {
            try {
                ProfessionAttribute attr = ProfessionAttribute.valueOf(attrName);
                double base = parseDouble(wrapper.getAttribute("base"), 0.0);
                double perLevel = parseDouble(wrapper.getAttribute("per_level"), 0.0);
                attributeMap.put(attr, new ProfessionAttributeNode.AttributeConfig(base, perLevel));
            } catch (IllegalArgumentException e) {
                log.error("Unknown ProfessionAttribute '{}' on node {}", attrName, id).submit();
            }
        }

        // Multiple attributes via indexed suffixes: attribute_0, attribute_1, ...
        for (int i = 0; ; i++) {
            String indexedAttr = wrapper.getAttribute("attribute_" + i);
            if (indexedAttr.isBlank()) break;

            try {
                ProfessionAttribute attr = ProfessionAttribute.valueOf(indexedAttr);
                double base = parseDouble(wrapper.getAttribute("base_" + i), 0.0);
                double perLevel = parseDouble(wrapper.getAttribute("per_level_" + i), 0.0);
                attributeMap.put(attr, new ProfessionAttributeNode.AttributeConfig(base, perLevel));
            } catch (IllegalArgumentException e) {
                log.error("Unknown ProfessionAttribute '{}' on node {}", indexedAttr, id).submit();
            }
        }

        return new ProfessionAttributeNode(id, maxLevel, attributeMap);
    }

    private boolean isRecipeNode(Element wrapper) {
        String nodeType = DrawioDocumentReader.nodeType(wrapper);
        return "recipe".equalsIgnoreCase(nodeType)
                || !DrawioDocumentReader.attributeValues(wrapper, "recipe").isEmpty()
                || !DrawioDocumentReader.attributeValues(wrapper, "recipes").isEmpty();
    }

    private ProfessionRecipeNode buildRecipeNode(String id, Element wrapper) {
        Set<Key> recipes = new LinkedHashSet<>();
        collectRecipeKeys(id, wrapper, "recipe", recipes);
        collectRecipeKeys(id, wrapper, "recipes", recipes);

        if (recipes.isEmpty()) {
            log.error("Recipe node {} has no recipes configured", id).submit();
            return null;
        }

        ProfessionRecipeNode node = new ProfessionRecipeNode(id, recipes);
        node.setMaxLevel(parseInt(wrapper.getAttribute("max_level"), 1));
        return node;
    }

    private void collectRecipeKeys(String id, Element wrapper, String attributeName, Set<Key> recipes) {
        for (String recipe : DrawioDocumentReader.attributeValues(wrapper, attributeName)) {
            try {
                recipes.add(Key.key(recipe));
            } catch (IllegalArgumentException e) {
                log.error("Invalid recipe key '{}' on node {}", recipe, id).submit();
            }
        }
    }

    private ProfessionNode buildSkillNode(String id, Element wrapper) {
        String skillClass = wrapper.getAttribute("skill_class");
        if (skillClass.isBlank()) {
            return null;
        }

        int maxLevel = parseInt(wrapper.getAttribute("max_level"), 1);

        try {
            Class<?> clazz = Class.forName(skillClass);
            if (!ProfessionSkill.class.isAssignableFrom(clazz)) {
                log.error("Class {} is not a ProfessionSkill (node node_id={})", skillClass, id).submit();
                return null;
            }

            ProfessionSkill skill = (ProfessionSkill) plugin.getInjector().getInstance(clazz);
            ProfessionSkillNode node = new ProfessionSkillNode(id, skill);
            node.setMaxLevel(maxLevel);
            plugin.getInjector().injectMembers(node);
            return node;
        } catch (ClassNotFoundException e) {
            log.error("Could not find class {} for node {}", skillClass, id).submit();
        } catch (Exception e) {
            log.error("Error creating skill node {} (class={}): {}", id, skillClass, e.getMessage()).submit();
        }

        return null;
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private record EdgeDependency(ProfessionNode parent, ProfessionNode child) {
    }
}
