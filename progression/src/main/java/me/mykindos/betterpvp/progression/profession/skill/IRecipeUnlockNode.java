package me.mykindos.betterpvp.progression.profession.skill;

import java.util.List;

public interface IRecipeUnlockNode {

    List<String> getUnlockedRecipeKeys();

    default int getUnlockLevel() {
        return 1;
    }

}
