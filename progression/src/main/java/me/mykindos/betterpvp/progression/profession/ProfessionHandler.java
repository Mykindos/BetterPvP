package me.mykindos.betterpvp.progression.profession;

import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;

import java.util.Optional;
import java.util.UUID;

@Singleton
public abstract class ProfessionHandler implements IProfession {

    protected final Progression progression;
    protected final ProfessionProfileManager professionProfileManager;
    protected final String profession;

    @Getter
    public boolean enabled;

    protected ProfessionHandler(Progression progression, ProfessionProfileManager professionProfileManager, String profession) {
        this.progression = progression;
        this.professionProfileManager = professionProfileManager;
        this.profession = profession;
    }

    public ProfessionData getProfessionData(UUID uuid) {
        Optional<ProfessionProfile> professionProfile = professionProfileManager.getObject(uuid.toString());
        if (professionProfile.isPresent()) {
            ProfessionProfile profile = professionProfile.get();
            return profile.getProfessionDataMap().computeIfAbsent(profession, k -> new ProfessionData(uuid, profession));
        }

        return null;
    }

    public void loadConfig() {
        this.enabled = progression.getConfig().getBoolean(profession + ".enabled", true);
    }

}
