package me.mykindos.betterpvp.progression.profile;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.event.PlayerProgressionExperienceEvent;
import me.mykindos.betterpvp.progression.event.ProfessionPropertyUpdateEvent;
import me.mykindos.betterpvp.progression.profession.skill.builds.ProgressionBuild;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;


@EqualsAndHashCode(callSuper = true)
@Data
public final class ProfessionData extends PropertyContainer implements IMapListener {

    private final UUID owner;
    private final String profession;

    private double experience;
    private ProgressionBuild build;

    public ProfessionData(UUID owner, String profession) {
        super();
        this.owner = owner;
        this.profession = profession;
        this.experience = 0;
        this.build = new ProgressionBuild(profession);
        properties.registerListener(this);

    }

    public void grantExperience(double amount, @Nullable Player player) {
        int previousLevel = getLevelFromExperience(experience);
        int newLevel = getLevelFromExperience(experience + amount);
        if (player != null) {
            UtilServer.callEvent(new PlayerProgressionExperienceEvent(player, profession, amount, newLevel, previousLevel, newLevel > previousLevel));
        }
    }

    public int getLevelFromExperience(double experience) {
        int level = 1;
        double expForNextLevel = 25;

        double experienceCopy = experience;

        while (experienceCopy >= expForNextLevel) {
            level++;
            experienceCopy -= expForNextLevel;
            expForNextLevel *= 1.01;
        }

        return level;
    }

    public double getExperienceForLevel(int level) {
        if (level < 2) {
            return 0;
        }

        double totalExperience = 0;
        double expForCurrentLevel = 25;

        for (int i = 2; i <= level; i++) {
            totalExperience += expForCurrentLevel;
            expForCurrentLevel *= 1.01;
        }

        return totalExperience;
    }

    @Override
    public void saveProperty(String key, Object object) {
        properties.put(key, object);
    }

    @Override
    public void onMapValueChanged(String key, Object value) {
        UtilServer.callEvent(new ProfessionPropertyUpdateEvent(owner, profession, key, value));
    }
}
