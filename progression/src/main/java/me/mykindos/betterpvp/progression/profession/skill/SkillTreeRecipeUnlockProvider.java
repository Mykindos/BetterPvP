package me.mykindos.betterpvp.progression.profession.skill;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.recipe.RecipeUnlockProvider;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
public class SkillTreeRecipeUnlockProvider implements RecipeUnlockProvider {

    private final ProfessionProfileManager professionProfileManager;

    @Inject
    public SkillTreeRecipeUnlockProvider(ProfessionProfileManager professionProfileManager) {
        this.professionProfileManager = professionProfileManager;
    }

    @Override
    public boolean isUnlocked(Player player, String recipeKey) {
        return professionProfileManager.getObject(player.getUniqueId().toString())
                .map(profile -> profile.getProfessionDataMap().values().stream()
                        .anyMatch(data -> data.getBuild().getNodes().entrySet().stream()
                                .anyMatch(entry -> {
                                    if (!(entry.getKey() instanceof IRecipeUnlockNode unlockNode)) return false;
                                    return entry.getValue() >= unlockNode.getUnlockLevel()
                                            && unlockNode.getUnlockedRecipeKeys().contains(recipeKey);
                                })))
                .orElse(false);
    }

    @Override
    public String getSource() {
        return "skill_tree";
    }

}
