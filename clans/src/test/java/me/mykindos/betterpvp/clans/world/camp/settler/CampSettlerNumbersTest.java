package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.wage.WageModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Wages and looks as the shipped settlers.yml gives them. */
class CampSettlerNumbersTest {

    private static double rate(WageModel model, String profession, SettlerRarity rarity, boolean working) {
        final Settler settler = new Settler();
        settler.setProfession(profession);
        settler.setRarity(rarity);
        return model.perHour(settler, working);
    }

    private static void onlyBuildersArePaid(SettlerConfig config) {
        for (SettlerRarity rarity : SettlerRarity.values()) {
            paysOnlyBuilders(config, rarity, false);
            paysOnlyBuilders(config, rarity, true);
        }
    }

    private static void paysOnlyBuilders(SettlerConfig config, SettlerRarity rarity, boolean working) {
        assertTrue(rate(config.getWageModel(), CampProfessions.BUILDER, rarity, working) > 0);
        assertEquals(0, rate(config.getWageModel(), CampProfessions.FARMER, rarity, working));
        assertEquals(0, rate(config.getWageModel(), null, rarity, working));
    }

    @Test
    void ac42_underBothShippedWageModelsOnlyBuildersArePaid() {
        final SettlerConfig fixed = ShippedSettlers.config();
        onlyBuildersArePaid(fixed);
        assertEquals(Duration.ofHours(72), fixed.getStrikeLimit());

        onlyBuildersArePaid(ShippedSettlers.config(yaml -> yaml.set("wages.model", "idle-working")));
    }

    @Test
    void ac43_eachLookLayersOverTheDefault() {
        final SettlerConfig config = ShippedSettlers.config();
        final SettlerLook builder = config.look(CampProfessions.BUILDER, SettlerRarity.COMMON);
        assertEquals("scene_market_1", builder.getModel());
        assertEquals("settler_builder", builder.getSkin());
        assertEquals("work", builder.getWorkAnimation());
        assertEquals("vendor_stand_2", builder.getIdleAnimation(), "from the default");

        assertEquals("settler_farmer", config.look(CampProfessions.FARMER, SettlerRarity.RARE).getSkin());
        assertEquals("settler_wanderer", config.look(null, SettlerRarity.LEGENDARY).getSkin());
        assertEquals("settler_steward", config.look("steward").getSkin());
        assertEquals("scene_market_1", config.look("steward").getModel());
        assertEquals("skin_farmer", config.defaultLook().getSkin());
    }

    @Test
    void ac43_aRarityLookOverridesItsProfessions() {
        final SettlerConfig config = ShippedSettlers.config(yaml -> {
            yaml.set("looks.builder.rarities.legendary.skin", "settler_builder_gold");
            yaml.set("looks.builder.rarities.legendary.size", 1.1);
        });
        final SettlerLook legendary = config.look(CampProfessions.BUILDER, SettlerRarity.LEGENDARY);
        assertEquals("settler_builder_gold", legendary.getSkin());
        assertEquals(1.1, legendary.getSize(), 1e-9);
        assertEquals("work", legendary.getWorkAnimation(), "the profession's still shows through");
        assertEquals("settler_builder", config.look(CampProfessions.BUILDER, SettlerRarity.RARE).getSkin());
        assertTrue(Arrays.stream(SettlerRarity.values()).allMatch(rarity ->
                config.look(CampProfessions.FARMER, rarity).getSkin().equals("settler_farmer")));
    }
}
