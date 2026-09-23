package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.Trait;
import me.mykindos.betterpvp.core.world.settler.TraitGroup;
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Every trait a camp's settlers can roll. What each one does is read by the system it affects, from its id, so this
 * only says who can roll it.
 */
@Singleton
public class CampTraits {

    public static final String STEADY_HANDS = "steady_hands";
    public static final String TIRELESS = "tireless";
    public static final String FRUGAL = "frugal";
    public static final String FOREMAN = "foreman";
    public static final String QUICK_STUDY = "quick_study";
    public static final String PATCHER = "patcher";
    public static final String ARCHITECTS_EYE = "architects_eye";
    public static final String LONER = "loner";
    public static final String GREEDY = "greedy";
    public static final String BRAWNY = "brawny";

    public static final String GREEN_THUMB = "green_thumb";
    public static final String BOUNTIFUL = "bountiful";
    public static final String SEASONED = "seasoned";
    public static final String MOODY = "moody";

    public static final String BARD = "bard";
    public static final String RECRUITER = "recruiter";
    public static final String HAGGLER = "haggler";
    public static final String QUARTERMASTER = "quartermaster";
    public static final String CHRONICLER = "chronicler";
    public static final String COOK = "cook";
    public static final String LOOKOUT = "lookout";
    public static final String BELOVED = "beloved";

    public static final String LOYAL = "loyal";
    public static final String CONTENT = "content";
    public static final String PRODIGY = "prodigy";
    public static final String HOMESICK = "homesick";

    @Inject
    public CampTraits(@NotNull TraitRegistry registry) {
        final List<Trait> traits = List.of(
                builder(STEADY_HANDS).build(),
                builder(TIRELESS).build(),
                builder(FRUGAL).build(),
                builder(FOREMAN).build(),
                builder(QUICK_STUDY).build(),
                builder(PATCHER).build(),
                builder(ARCHITECTS_EYE).build(),
                builder(LONER).tradeOff(true).build(),
                builder(GREEDY).tradeOff(true).build(),
                builder(BRAWNY).tradeOff(true).build(),

                resident(GREEN_THUMB).build(),
                resident(BOUNTIFUL).build(),
                resident(SEASONED).build(),
                resident(MOODY).tradeOff(true).build(),

                trait(BARD, TraitGroup.SITE_WIDE).build(),
                trait(RECRUITER, TraitGroup.SITE_WIDE).build(),
                trait(HAGGLER, TraitGroup.SITE_WIDE).build(),
                trait(QUARTERMASTER, TraitGroup.SITE_WIDE).build(),
                trait(CHRONICLER, TraitGroup.SITE_WIDE).build(),
                trait(COOK, TraitGroup.SITE_WIDE).build(),
                trait(LOOKOUT, TraitGroup.SITE_WIDE).minimumRarity(SettlerRarity.RARE).build(),
                trait(BELOVED, TraitGroup.SITE_WIDE).minimumRarity(SettlerRarity.LEGENDARY).build(),

                trait(LOYAL, TraitGroup.PERSONAL).build(),
                trait(CONTENT, TraitGroup.PERSONAL).build(),
                professional(PRODIGY).minimumRarity(SettlerRarity.LEGENDARY).build(),
                professional(HOMESICK).tradeOff(true).build());
        traits.forEach(registry::register);
    }

    private static Trait.TraitBuilder trait(@NotNull String id, @NotNull TraitGroup group) {
        return Trait.builder().id(id).key("clans.settler.trait." + id).group(group);
    }

    private static Trait.TraitBuilder builder(@NotNull String id) {
        return trait(id, TraitGroup.BUILDER).profession(CampProfessions.BUILDER);
    }

    /** Works at a workplace, which only Farmers do so far. */
    private static Trait.TraitBuilder resident(@NotNull String id) {
        return trait(id, TraitGroup.RESIDENT).profession(CampProfessions.FARMER);
    }

    /** Changes profession stats, so only settlers with a profession roll it. */
    private static Trait.TraitBuilder professional(@NotNull String id) {
        return trait(id, TraitGroup.PERSONAL).profession(CampProfessions.BUILDER).profession(CampProfessions.FARMER);
    }
}
