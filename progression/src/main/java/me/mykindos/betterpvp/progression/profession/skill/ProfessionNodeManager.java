package me.mykindos.betterpvp.progression.profession.skill;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@CustomLog
public class ProfessionNodeManager extends Manager<String, ProfessionNode> {

    private final Progression progression;
    private final List<ProfessionHandler> handlers;

    @Inject
    public ProfessionNodeManager(Progression progression, FishingHandler fishingHandler,
                                 WoodcuttingHandler woodcuttingHandler, MiningHandler miningHandler) {
        this.progression = progression;
        this.handlers = List.of(fishingHandler, woodcuttingHandler, miningHandler);
    }

    public void loadSkills() {
        for (ProfessionHandler handler : handlers) {
            String profession = handler.getName();
            List<ProfessionNode> nodes = handler.getNodeLoaderStrategy().loadNodes(progression, profession);
            for (ProfessionNode node : nodes) {
                addObject(node.getName(), node);
            }
            Set<ProfessionSkill> skills = nodes.stream()
                    .map(ProfessionNode::getSkill)
                    .filter(skill -> skill != null)
                    .collect(Collectors.toSet());
            for (ProfessionSkill skill : skills) {
                skill.initialize(profession);
                skill.loadSkillConfig();
            }
            log.info("Loaded " + nodes.size() + " profession nodes for " + profession).submit();
        }

        log.info("Loaded " + objects.size() + " profession nodes").submit();
        progression.saveConfig();
    }

    public void reload() {
        getObjects().clear();
        loadSkills();
    }

    public void reloadSkills() {
        getObjects().values().forEach(ProfessionNode::reload);
    }

    public Optional<ProfessionNode> getSkill(String name) {
        return Optional.ofNullable(objects.get(name));
    }
}
