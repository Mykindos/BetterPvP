package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.scythe.ScytheOfTheFallenLord;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ChampionsItemBoostrap {

    private final Champions champions;
    private final ItemRegistry itemRegistry;

    @Inject
    private ChampionsItemBoostrap(Champions champions, ItemRegistry itemRegistry) {
        this.champions = champions;
        this.itemRegistry = itemRegistry;
    }

    @Inject
    private void registerConsumables(EnergyApple energyApple,
                                     EnergyElixir energyElixir,
                                     MushroomStew mushroomStew,
                                     PurificationPotion purificationPotion,
                                     RabbitStew rabbitStew,
                                     SuspiciousStew suspiciousStew,
                                     ThrowingWeb throwingWeb) {
        itemRegistry.registerItem(championsKey("energy_apple"), energyApple);
        itemRegistry.registerFallbackItem(championsKey("energy_elixir"), Material.POTION, energyElixir);
        itemRegistry.registerFallbackItem(championsKey("mushroom_stew"), Material.MUSHROOM_STEW, mushroomStew);
        itemRegistry.registerItem(championsKey("purification_potion"), purificationPotion);
        itemRegistry.registerFallbackItem(championsKey("rabbit_stew"), Material.RABBIT_STEW, rabbitStew);
        itemRegistry.registerItem(championsKey("suspicious_stew"), suspiciousStew);
        itemRegistry.registerFallbackItem(championsKey("throwing_web"), Material.COBWEB, throwingWeb);
    }

    @Inject
    private void registerWeapons(AncientSword ancientSword,
                                 AncientAxe ancientAxe,
                                 PowerSword powerSword,
                                 PowerAxe powerAxe,
                                 BoosterSword boosterSword,
                                 BoosterAxe boosterAxe,
                                 StandardSword standardSword,
                                 StandardAxe standardAxe) {
        itemRegistry.registerFallbackItem(championsKey("ancient_sword"), Material.NETHERITE_SWORD, ancientSword);
        itemRegistry.registerFallbackItem(championsKey("ancient_axe"), Material.NETHERITE_AXE, ancientAxe);
        itemRegistry.registerFallbackItem(championsKey("power_sword"), Material.DIAMOND_SWORD, powerSword);
        itemRegistry.registerFallbackItem(championsKey("power_axe"), Material.DIAMOND_AXE, powerAxe);
        itemRegistry.registerFallbackItem(championsKey("booster_sword"), Material.GOLDEN_SWORD, boosterSword);
        itemRegistry.registerFallbackItem(championsKey("booster_axe"), Material.GOLDEN_AXE, boosterAxe);
        itemRegistry.registerFallbackItem(championsKey("standard_sword"), Material.IRON_SWORD, standardSword);
        itemRegistry.registerFallbackItem(championsKey("standard_axe"), Material.IRON_AXE, standardAxe);
    }

    @Inject
    private void registerLegendaries(MagneticMaul magneticMaul,
                                     GiantsBroadsword giantsBroadsword,
                                     HyperAxe hyperAxe,
                                     WindBlade windBlade,
                                     MeridianScepter meridianScepter,
                                     ScytheOfTheFallenLord scythe,
                                     ThunderclapAegis thunderclapAegis,
                                     AlligatorsTooth alligatorsTooth,
                                     Rake rake,
                                     RunedPickaxe runedPickaxe) {
        itemRegistry.registerItem(championsKey("magnetic_maul"), magneticMaul);
        itemRegistry.registerItem(championsKey("giants_broadsword"), giantsBroadsword);
        itemRegistry.registerItem(championsKey("hyper_axe"), hyperAxe);
        itemRegistry.registerItem(championsKey("wind_blade"), windBlade);
        itemRegistry.registerItem(championsKey("meridian_scepter"), meridianScepter);
        itemRegistry.registerItem(championsKey("scythe"), scythe);
        itemRegistry.registerItem(championsKey("thunderclap_aegis"), thunderclapAegis);
        itemRegistry.registerItem(championsKey("alligators_tooth"), alligatorsTooth);
        itemRegistry.registerItem(championsKey("rake"), rake);
        itemRegistry.registerItem(championsKey("runed_pickaxe"), runedPickaxe);
    }

    @Inject
    private void registerMythicals(Mjolnir mjolnir) {
        itemRegistry.registerItem(championsKey("mjolnir"), mjolnir);
    }

    @Inject
    private void registerArmor(AssassinHelmet assassinHelmet,
                               AssassinChestplate assassinChestplate,
                               AssassinLeggings assassinLeggings,
                               AssassinBoots assassinBoots,
                               BruteHelmet bruteHelmet,
                               BruteChestplate bruteChestplate,
                               BruteLeggings bruteLeggings,
                               BruteBoots bruteBoots,
                               KnightHelmet knightHelmet,
                               KnightChestplate knightChestplate,
                               KnightLeggings knightLeggings,
                               KnightBoots knightBoots,
                               MageHelmet mageHelmet,
                               MageChestplate mageChestplate,
                               MageLeggings mageLeggings,
                               MageBoots mageBoots,
                               RangerHelmet rangerHelmet,
                               RangerChestplate rangerChestplate,
                               RangerLeggings rangerLeggings,
                               RangerBoots rangerBoots,
                               WarlockHelmet warlockHelmet,
                               WarlockChestplate warlockChestplate,
                               WarlockLeggings warlockLeggings,
                               WarlockBoots warlockBoots) {
        itemRegistry.registerFallbackItem(championsKey("assassin_helmet"), Material.LEATHER_HELMET, assassinHelmet);
        itemRegistry.registerFallbackItem(championsKey("assassin_chestplate"), Material.LEATHER_CHESTPLATE, assassinChestplate);
        itemRegistry.registerFallbackItem(championsKey("assassin_leggings"), Material.LEATHER_LEGGINGS, assassinLeggings);
        itemRegistry.registerFallbackItem(championsKey("assassin_boots"), Material.LEATHER_BOOTS, assassinBoots);
        itemRegistry.registerFallbackItem(championsKey("brute_helmet"), Material.DIAMOND_HELMET, bruteHelmet);
        itemRegistry.registerFallbackItem(championsKey("brute_chestplate"), Material.DIAMOND_CHESTPLATE, bruteChestplate);
        itemRegistry.registerFallbackItem(championsKey("brute_leggings"), Material.DIAMOND_LEGGINGS, bruteLeggings);
        itemRegistry.registerFallbackItem(championsKey("brute_boots"), Material.DIAMOND_BOOTS, bruteBoots);
        itemRegistry.registerFallbackItem(championsKey("knight_helmet"), Material.IRON_HELMET, knightHelmet);
        itemRegistry.registerFallbackItem(championsKey("knight_chestplate"), Material.IRON_CHESTPLATE, knightChestplate);
        itemRegistry.registerFallbackItem(championsKey("knight_leggings"), Material.IRON_LEGGINGS, knightLeggings);
        itemRegistry.registerFallbackItem(championsKey("knight_boots"), Material.IRON_BOOTS, knightBoots);
        itemRegistry.registerFallbackItem(championsKey("mage_helmet"), Material.GOLDEN_HELMET, mageHelmet);
        itemRegistry.registerFallbackItem(championsKey("mage_chestplate"), Material.GOLDEN_CHESTPLATE, mageChestplate);
        itemRegistry.registerFallbackItem(championsKey("mage_leggings"), Material.GOLDEN_LEGGINGS, mageLeggings);
        itemRegistry.registerFallbackItem(championsKey("mage_boots"), Material.GOLDEN_BOOTS, mageBoots);
        itemRegistry.registerFallbackItem(championsKey("ranger_helmet"), Material.CHAINMAIL_HELMET, rangerHelmet);
        itemRegistry.registerFallbackItem(championsKey("ranger_chestplate"), Material.CHAINMAIL_CHESTPLATE, rangerChestplate);
        itemRegistry.registerFallbackItem(championsKey("ranger_leggings"), Material.CHAINMAIL_LEGGINGS, rangerLeggings);
        itemRegistry.registerFallbackItem(championsKey("ranger_boots"), Material.CHAINMAIL_BOOTS, rangerBoots);
        itemRegistry.registerFallbackItem(championsKey("warlock_helmet"), Material.NETHERITE_HELMET, warlockHelmet);
        itemRegistry.registerFallbackItem(championsKey("warlock_chestplate"), Material.NETHERITE_CHESTPLATE, warlockChestplate);
        itemRegistry.registerFallbackItem(championsKey("warlock_leggings"), Material.NETHERITE_LEGGINGS, warlockLeggings);
        itemRegistry.registerFallbackItem(championsKey("warlock_boots"), Material.NETHERITE_BOOTS, warlockBoots);
    }

    private @NotNull NamespacedKey championsKey(@NotNull String key) {
        return new NamespacedKey(champions, key);
    }

}
