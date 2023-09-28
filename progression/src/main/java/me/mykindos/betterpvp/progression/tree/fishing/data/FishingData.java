package me.mykindos.betterpvp.progression.tree.fishing.data;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.progression.model.ProgressionData;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.fish.Fish;

@Getter
@Setter
public final class FishingData extends ProgressionData<Fishing> {

    // We store A LOT more data than this, but we don't want to load it all into memory for everybody
    // We *could*, in the future, load people's data upon request and cache it for some time, so it's
    // not as resource-intensive, but for now, we'll just store the bare minimum.
    private int fishCaught;

    public FishingData() {
        this.fishCaught = 0;
    }

    public void addFish(Fish fish) {
        this.fishCaught++;
    }

}
