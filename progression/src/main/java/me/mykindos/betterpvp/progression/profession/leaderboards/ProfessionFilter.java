package me.mykindos.betterpvp.progression.profession.leaderboards;

import lombok.Getter;
import me.mykindos.betterpvp.core.stats.filter.FilterType;
import org.jetbrains.annotations.NotNull;

@Getter
public final class ProfessionFilter implements FilterType {

    private final String profession;
    private static final FilterType[] values = new ProfessionFilter[]{
            new ProfessionFilter("Fishing"),
            new ProfessionFilter("Woodcutting"),
            new ProfessionFilter("Mining")

    };

    public ProfessionFilter(String profession) {
        this.profession = profession;
    }


    public static FilterType[] values() {
        return values;
    }

    @NotNull
    @Override
    public String getName() {
        return profession;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ProfessionFilter that = (ProfessionFilter) obj;
        return this.profession.equals(that.profession);
    }

    @Override
    public boolean accepts(Object entry) {
        return entry instanceof ProfessionFilter run && run.profession.equals(this.profession);
    }
}
