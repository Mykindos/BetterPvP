package me.mykindos.betterpvp.progression.profile;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.progression.profile.repository.ProfessionProfileRepository;

import java.util.UUID;

@Singleton
public class ProfessionProfileManager extends Manager<ProfessionProfile> {

    @Getter
    private final ProfessionProfileRepository repository;

    @Inject
    public ProfessionProfileManager(ProfessionProfileRepository repository) {
        this.repository = repository;
    }

    public void loadProfile(UUID uuid) {
        addObject(uuid.toString(), repository.loadProfileForGamer(uuid));
    }

}
