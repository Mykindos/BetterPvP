package me.mykindos.betterpvp.progression.profession.skill.loader;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNode;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;

import java.util.ArrayList;
import java.util.List;

@CustomLog
public class YamlNodeLoaderStrategy implements NodeLoaderStrategy {

    @Override
    public List<ProfessionNode> loadNodes(Progression progression, String profession) {
        List<ProfessionNode> nodes = new ArrayList<>();
        String professionName = profession.toLowerCase();
        ExtendedYamlConfiguration config = progression.getConfig("professions/" + professionName + "/" + professionName);
        var nodesSection = config.getConfigurationSection("nodes");

        if (nodesSection == null) {
            return nodes;
        }

        for (String nodeName : nodesSection.getKeys(false)) {
            String classPath = null;
            var nodeSection = nodesSection.getConfigurationSection(nodeName);
            if (nodeSection != null) {
                classPath = nodeSection.getString("class");
            }

            if (classPath == null || classPath.isEmpty()) {
                log.error("No class path defined for node {} in profession {}", nodeName, professionName).submit();
                continue;
            }

            Class<?> nodeClass = findNodeClass(nodeName, classPath);
            if (nodeClass == null) {
                continue;
            }

            ProfessionNode node = createNodeInstance(progression, nodeClass, nodeName);
            if (node == null) {
                continue;
            }

            node.initialize(profession);
            nodes.add(node);
        }

        return nodes;
    }

    private Class<?> findNodeClass(String nodeName, String classPath) {
        try {
            return Class.forName(classPath);
        } catch (ClassNotFoundException e) {
            log.error("Could not find class {} for node {}", classPath, nodeName).submit();
            return null;
        }
    }

    private ProfessionNode createNodeInstance(Progression progression, Class<?> nodeClass, String nodeName) {
        try {
            if (ProfessionSkill.class.isAssignableFrom(nodeClass)) {
                ProfessionSkill skill = (ProfessionSkill) progression.getInjector().getInstance(nodeClass);
                ProfessionSkillNode node = new ProfessionSkillNode(nodeName, skill);
                progression.getInjector().injectMembers(node);
                return node;
            }

            if (!ProfessionNode.class.isAssignableFrom(nodeClass)) {
                log.error("Class {} is not a ProfessionNode or ProfessionSkill", nodeClass.getName()).submit();
                return null;
            }

            ProfessionNode node = (ProfessionNode) nodeClass.getDeclaredConstructor(String.class).newInstance(nodeName);
            progression.getInjector().injectMembers(node);
            return node;
        } catch (Exception e) {
            log.error("Error creating instance of {}: {}", nodeClass.getName(), e.getMessage()).submit();
            return null;
        }
    }
}
