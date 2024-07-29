package me.mykindos.betterpvp.clans.clans.pillage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.utilities.UtilMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class PillageHandler {

    @Getter
    private final List<Pillage> activePillages = new ArrayList<>();

    private final Clans clans;



    @Inject
    public PillageHandler(Clans clans) {
        this.clans = clans;
    }

    public List<Pillage> getPillagesOn(IClan clan) {
        return activePillages.stream().filter(pillage -> pillage.getPillaged().equals(clan)).toList();
    }

    public List<Pillage> getPillagesBy(IClan clan) {
        return activePillages.stream().filter(pillage -> pillage.getPillager().equals(clan)).toList();
    }

    public boolean isPillaging(IClan pillager, IClan pillaged) {
        return getPillage(pillager, pillaged).isPresent();
    }

    public Optional<Pillage> getPillage(IClan pillager, IClan pillaged) {
        return activePillages.stream().filter(pillage -> pillage.getPillager().equals(pillager)
                && pillage.getPillaged().equals(pillaged)).findFirst();
    }

    public boolean isBeingPillaged(IClan clan) {
        return activePillages.stream().anyMatch(pillage -> pillage.getPillaged().equals(clan));
    }

    public void startPillage(Pillage pillage) {
        activePillages.add(pillage);
    }

    public void endPillage(Pillage pillage) {
        activePillages.remove(pillage);
        UtilMessage.simpleBroadcast("Clans", "The pillage on <yellow>%s <gray>has ended.",
                pillage.getPillaged().getName());
    }
}
