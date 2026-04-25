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
import org.reflections.Reflections;

import javax.inject.Provider;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@CustomLog
public class ProfessionNodeManager extends Manager<String, ProfessionNode> {

    private final Progression progression;
    private final List<ProfessionHandler> handlers;
    private final Provider<SkillTreeAccessProvider> accessProviderProvider;
    private final Map<String, IProfessionSkill> skillsByNodeId = new HashMap<>();
    private final Map<String, IProfessionAttribute> attributesByNodeId = new HashMap<>();

    @Inject
    public ProfessionNodeManager(Progression progression, FishingHandler fishingHandler,
                                 WoodcuttingHandler woodcuttingHandler, MiningHandler miningHandler,
                                 Provider<SkillTreeAccessProvider> accessProviderProvider) {
        this.progression = progression;
        this.handlers = List.of(fishingHandler, woodcuttingHandler, miningHandler);
        this.accessProviderProvider = accessProviderProvider;
    }

    public void loadNodeRegistry() {
        skillsByNodeId.clear();
        attributesByNodeId.clear();

        Reflections reflections = new Reflections("me.mykindos.betterpvp.progression");
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(NodeId.class);

        for (Class<?> clazz : classes) {
            NodeId nodeId = clazz.getAnnotation(NodeId.class);
            if (nodeId == null) {
                continue;
            }

            if (IProfessionSkill.class.isAssignableFrom(clazz) && isConcrete(clazz)) {
                @SuppressWarnings("unchecked")
                Class<? extends IProfessionSkill> skillClass = (Class<? extends IProfessionSkill>) clazz;
                IProfessionSkill skill = progression.getInjector().getInstance(skillClass);
                skillsByNodeId.put(nodeId.value(), skill);
                continue;
            }

            if (IProfessionAttribute.class.isAssignableFrom(clazz) && isConcrete(clazz)) {
                try {
                    Class<? extends IProfessionAttribute> attributeClass = clazz.asSubclass(IProfessionAttribute.class);
                    IProfessionAttribute attribute = progression.getInjector().getInstance(attributeClass);
                    attributesByNodeId.put(normalizeAttributeNodeId(nodeId.value()), attribute);
                } catch (Exception e) {
                    log.warn("Failed to instantiate profession attribute {} for node id {}: {}", clazz.getName(), nodeId.value(), e.getMessage()).submit();
                }
                continue;
            }

            log.warn("Class {} is annotated with @NodeId but is not a ProfessionSkill or IProfessionAttribute", clazz.getName()).submit();
        }

        log.info("Loaded {} profession skills and {} profession attributes into node registry", skillsByNodeId.size(), attributesByNodeId.size()).submit();
    }

    private boolean isConcrete(Class<?> clazz) {
        int modifiers = clazz.getModifiers();
        return !clazz.isInterface() && !Modifier.isAbstract(modifiers);
    }

    public void loadSkills() {
        for (ProfessionHandler handler : handlers) {
            String profession = handler.getName();
            List<ProfessionNode> nodes = handler.getNodeLoaderStrategy().loadNodes(progression, profession);
            for (ProfessionNode node : nodes) {
                addObject(node.getName(), node);
            }
            Set<IProfessionSkill> skills = nodes.stream()
                    .map(ProfessionNode::getSkill)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (IProfessionSkill skill : skills) {
                if (skill instanceof ProfessionSkill professionSkill) {
                    professionSkill.initialize(profession);
                    professionSkill.loadSkillConfig();
                }
            }
            log.info("Loaded " + nodes.size() + " profession nodes for " + profession).submit();
        }

        log.info("Loaded " + objects.size() + " profession nodes").submit();
        progression.saveConfig();
        accessProviderProvider.get().rebuildIndex();
    }

    public void reload() {
        getObjects().clear();
        loadNodeRegistry();
        loadSkills();
    }

    public void reloadSkills() {
        getObjects().values().forEach(ProfessionNode::reload);
    }

    public Optional<ProfessionNode> getSkill(String name) {
        return Optional.ofNullable(objects.get(name));
    }

    public Optional<IProfessionSkill> getSkillByNodeId(String id) {
        return Optional.ofNullable(skillsByNodeId.get(id));
    }

    public Optional<IProfessionAttribute> getAttributeByNodeId(String id) {
        return Optional.ofNullable(attributesByNodeId.get(normalizeAttributeNodeId(id)));
    }

    private String normalizeAttributeNodeId(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}
