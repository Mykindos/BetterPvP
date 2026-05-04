package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.oreinfusion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("ore_infusion")
public class OreInfusionAttribute implements IProfessionAttribute, Reloadable {

    private final Progression progression;
    private final ProfessionProfileManager profileManager;
    @Getter
    private double oreChance;
    @Getter
    private double radius;

    @Inject
    public OreInfusionAttribute(Progression progression, ProfessionProfileManager profileManager) {
        this.progression = progression;
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Ore Infusion";
    }

    @Override
    public String getDescription() {
        return "chance for nearby stone blocks to convert into ore when mining";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100.0;
    }

    public double getChance(Player player) {
        return IProfessionAttribute.computeValue(player, "Mining", this, profileManager);
    }

    @Override
    public void reload() {
        this.oreChance = getConfig("ore-chance", 0.01, Double.class);
        this.radius = getConfig("radius", 10.0, Double.class);
    }

    protected <T> T getConfig(String key, Object defaultValue, Class<T> type) {
        String path = "attributes.ore-infusion." + key;
        return progression.getConfig("professions/mining/mining").getOrSaveObject(path, defaultValue, type);
    }
}
