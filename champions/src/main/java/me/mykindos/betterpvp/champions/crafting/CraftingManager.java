package me.mykindos.betterpvp.champions.crafting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.crafting.imbuements.Imbuement;
import me.mykindos.betterpvp.champions.crafting.imbuements.ImbuementRepository;

import java.util.List;

@Singleton
public class CraftingManager {


    @Getter
    private List<Imbuement> imbuements;

    @Inject
    public CraftingManager(ImbuementRepository imbuementRepository) {
        imbuements = imbuementRepository.getAll();
    }


}
