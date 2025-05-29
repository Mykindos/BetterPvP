package me.mykindos.betterpvp.progression.profession.skill;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.Professions;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;



@Singleton
@CustomLog
public class ProfessionNodeManager extends Manager<ProfessionNode> {

    private final Progression progression;

    @Inject
    public ProfessionNodeManager(Progression progression) {
        this.progression = progression;
    }

    /**
     * Load all profession nodes from configuration
     * All nodes are enabled by default
     */
    public void loadSkills() {
        // Load node definitions from configuration
        Map<String, NodeDefinition> nodeDefinitions = loadNodeDefinitions();

        // Create node instances from definitions
        for (Map.Entry<String, NodeDefinition> entry : nodeDefinitions.entrySet()) {
            String nodeName = entry.getKey();
            NodeDefinition definition = entry.getValue();
            String profession = definition.profession();
            String classPath = definition.classPath();

            try {
                // Try to find the node class by name
                Class<?> nodeClass = findNodeClass(nodeName, classPath);
                if (nodeClass == null) {
                    log.error("Could not find class for node {}", nodeName).submit();
                    continue;
                }

                // Create an instance of the node
                ProfessionNode node = createNodeInstance(nodeClass, nodeName);
                if (node == null) {
                    System.out.println("Failed " + nodeName);
                    continue;
                }

                // Initialize the node with the profession
                node.initialize(profession);

                System.out.println(nodeName);
                System.out.println(node.getName());
                // Add the node to the manager
                addObject(node.getName(), node);

            } catch (Exception e) {
                log.error("Error loading profession node {}: {}", nodeName, e.getMessage()).submit();
            }
        }

        System.out.println(objects.containsKey("base_fishing"));

        log.info("Loaded " + objects.size() + " profession nodes").submit();
        progression.saveConfig();
    }

    /**
     * Load node definitions from configuration
     * @return A map of node names to node definitions
     */
    private Map<String, NodeDefinition> loadNodeDefinitions() {
        Map<String, NodeDefinition> nodeDefinitions = new HashMap<>();

        // Load from each profession config
        for (Professions profession : Professions.values()) {
            String professionName = profession.name().toLowerCase();
            ExtendedYamlConfiguration config = progression.getConfig("professions/" + professionName + "/" + professionName);
            var nodesSection = config.getConfigurationSection("nodes");

            if (nodesSection != null) {
                for (String nodeName : nodesSection.getKeys(false)) {
                    // Get the class path from the config if available
                    String classPath = null;
                    var nodeSection = nodesSection.getConfigurationSection(nodeName);
                    if (nodeSection != null) {
                        classPath = nodeSection.getString("class");
                    }

                    nodeDefinitions.put(nodeName, new NodeDefinition(professionName, classPath));
                }
            }
        }

        return nodeDefinitions;
    }

    /**
     * Find a node class by name and optional class path
     * @param nodeName The name of the node
     * @param classPath The full class path of the node (optional)
     * @return The node class, or null if not found
     */
    private Class<?> findNodeClass(String nodeName, String classPath) {
        // If class path is provided, try to load it directly
        if (classPath != null && !classPath.isEmpty()) {
            try {
                return Class.forName(classPath);
            } catch (ClassNotFoundException e) {
                log.error("Could not find class {} for node {}", classPath, nodeName).submit();
                return null;
            }
        }

        return null;
    }

    /**
     * Create an instance of a node
     * @param nodeClass The node class
     * @param nodeName The name of the node
     * @return The node instance, or null if creation failed
     */
    private ProfessionNode createNodeInstance(Class<?> nodeClass, String nodeName) {
        try {
            // Check if the class is a ProfessionNode
            if (!ProfessionNode.class.isAssignableFrom(nodeClass)) {
                log.error("Class {} is not a ProfessionNode", nodeClass.getName()).submit();
                return null;
            }

            // Find constructor that takes a String parameter
            Constructor<?> constructor = nodeClass.getDeclaredConstructor(String.class);

            // Create instance
            ProfessionNode node = (ProfessionNode) constructor.newInstance(nodeName);

            // Inject dependencies
            progression.getInjector().injectMembers(node);

            return node;
        } catch (Exception e) {
            log.error("Error creating instance of {}: {}", nodeClass.getName(), e.getMessage()).submit();
            return null;
        }
    }

    public void reload() {
        getObjects().clear();
        loadSkills();
    }

    public void reloadSkills(){
        getObjects().values().forEach(ProfessionNode::reload);
    }

    public Optional<ProfessionNode> getSkill(String name){
        return Optional.ofNullable(objects.get(name));
    }

}
