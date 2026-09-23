package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** The professions a camp's settlers can have. */
@Singleton
public class CampProfessions {

    public static final String BUILDER = "builder";
    public static final String FARMER = "farmer";

    public static final String MASON = "mason";
    public static final String CARPENTER = "carpenter";
    public static final String SMITH = "smith";
    public static final String LABORER = "laborer";

    @Inject
    public CampProfessions(@NotNull ProfessionRegistry registry) {
        registry.register(Profession.construction(BUILDER, key(BUILDER),
                List.of(MASON, CARPENTER, SMITH, LABORER)));
        registry.register(Profession.workplace(FARMER, key(FARMER), CampGrounds.FARM));
    }

    private static @NotNull String key(@NotNull String profession) {
        return "clans.settler.profession." + profession;
    }
}
