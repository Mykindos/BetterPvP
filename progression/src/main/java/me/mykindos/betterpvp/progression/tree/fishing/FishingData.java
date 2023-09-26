package me.mykindos.betterpvp.progression.tree.fishing;

import com.google.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.progression.model.ProgressionData;

@Getter
@Setter
public final class FishingData extends ProgressionData<Fishing> {

    @Inject
    public FishingData(Fishing tree) {
        super(tree);
    }

}
