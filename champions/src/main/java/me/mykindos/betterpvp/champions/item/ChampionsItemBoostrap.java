package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.bloomrot.Bloomrot;
import me.mykindos.betterpvp.champions.item.ivybolt.Ivybolt;
import me.mykindos.betterpvp.champions.item.scythe.ScytheOfTheFallenLord;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.minecraft.MinecraftCraftingRecipeAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ChampionsItemBoostrap {

    private final Champions champions;
    private final ItemRegistry itemRegistry;
    private final CraftingRecipeRegistry registry;
    private final MinecraftCraftingRecipeAdapter adapter;

    @Inject
    private ChampionsItemBoostrap(Champions champions, ItemRegistry itemRegistry, CraftingRecipeRegistry registry, MinecraftCraftingRecipeAdapter adapter) {
        this.champions = champions;
        this.itemRegistry = itemRegistry;
        this.registry = registry;
        this.adapter = adapter;
    }

    @Inject
    private void registerConsumables(EnergyApple energyApple,
                                     EnergyElixir energyElixir,
                                     MushroomStew mushroomStew,
                                     PurificationPotion purificationPotion,
                                     RabbitStew rabbitStew,
                                     SuspiciousStew suspiciousStew,
                                     ThrowingWeb throwingWeb) {
        registerFallbackItem("energy_apple", Material.APPLE, energyApple, false);
        registerFallbackItem("energy_elixir", Material.POTION, energyElixir, false);
        registerFallbackItem("mushroom_stew", Material.MUSHROOM_STEW, mushroomStew, false);
        itemRegistry.registerItem(championsKey("purification_potion"), purificationPotion);
        registerFallbackItem("rabbit_stew", Material.RABBIT_STEW, rabbitStew, false);
        registerFallbackItem("suspicious_stew", Material.SUSPICIOUS_STEW, suspiciousStew, false);
        registerFallbackItem("throwing_web", Material.COBWEB, throwingWeb, false);
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
        registerFallbackItem("ancient_sword", Material.NETHERITE_SWORD, ancientSword, false);
        registerFallbackItem("ancient_axe", Material.NETHERITE_AXE, ancientAxe, false);
        registerFallbackItem("power_sword", Material.DIAMOND_SWORD, powerSword, false);
        registerFallbackItem("power_axe", Material.DIAMOND_AXE, powerAxe, false);
        registerFallbackItem("booster_sword", Material.GOLDEN_SWORD, boosterSword, false);
        registerFallbackItem("booster_axe", Material.GOLDEN_AXE, boosterAxe, false);
        registerFallbackItem("standard_sword", Material.IRON_SWORD, standardSword, true);
        registerFallbackItem("standard_axe", Material.IRON_AXE, standardAxe, true);
    }

    @Inject
    private void registerSpecials(MagneticMaul magneticMaul,
                                  GiantsBroadsword giantsBroadsword,
                                  HyperAxe hyperAxe,
                                  WindBlade windBlade,
                                  MeridianScepter meridianScepter,
                                  ScytheOfTheFallenLord scythe,
                                  ThunderclapAegis thunderclapAegis,
                                  AlligatorsTooth alligatorsTooth,
                                  Rake rake,
                                  RunedPickaxe runedPickaxe,
                                  Bloomrot bloomrot,
                                  Ivybolt ivybolt) {
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
        itemRegistry.registerItem(championsKey("bloomrot"), bloomrot);
        itemRegistry.registerItem(championsKey("ivybolt"), ivybolt);
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
        registerFallbackItem("assassin_helmet", Material.LEATHER_HELMET, assassinHelmet, true);
        registerFallbackItem("assassin_chestplate", Material.LEATHER_CHESTPLATE, assassinChestplate, true);
        registerFallbackItem("assassin_leggings", Material.LEATHER_LEGGINGS, assassinLeggings, true);
        registerFallbackItem("assassin_boots", Material.LEATHER_BOOTS, assassinBoots, true);
        registerFallbackItem("brute_helmet", Material.DIAMOND_HELMET, bruteHelmet, true);
        registerFallbackItem("brute_chestplate", Material.DIAMOND_CHESTPLATE, bruteChestplate, true);
        registerFallbackItem("brute_leggings", Material.DIAMOND_LEGGINGS, bruteLeggings, true);
        registerFallbackItem("brute_boots", Material.DIAMOND_BOOTS, bruteBoots, true);
        registerFallbackItem("knight_helmet", Material.IRON_HELMET, knightHelmet, true);
        registerFallbackItem("knight_chestplate", Material.IRON_CHESTPLATE, knightChestplate, true);
        registerFallbackItem("knight_leggings", Material.IRON_LEGGINGS, knightLeggings, true);
        registerFallbackItem("knight_boots", Material.IRON_BOOTS, knightBoots, true);
        registerFallbackItem("mage_helmet", Material.GOLDEN_HELMET, mageHelmet, true);
        registerFallbackItem("mage_chestplate", Material.GOLDEN_CHESTPLATE, mageChestplate, true);
        registerFallbackItem("mage_leggings", Material.GOLDEN_LEGGINGS, mageLeggings, true);
        registerFallbackItem("mage_boots", Material.GOLDEN_BOOTS, mageBoots, true);
        registerFallbackItem("ranger_helmet", Material.CHAINMAIL_HELMET, rangerHelmet, false);
        registerFallbackItem("ranger_chestplate", Material.CHAINMAIL_CHESTPLATE, rangerChestplate, false);
        registerFallbackItem("ranger_leggings", Material.CHAINMAIL_LEGGINGS, rangerLeggings, false);
        registerFallbackItem("ranger_boots", Material.CHAINMAIL_BOOTS, rangerBoots, false);
        registerFallbackItem("warlock_helmet", Material.NETHERITE_HELMET, warlockHelmet, true);
        registerFallbackItem("warlock_chestplate", Material.NETHERITE_CHESTPLATE, warlockChestplate, true);
        registerFallbackItem("warlock_leggings", Material.NETHERITE_LEGGINGS, warlockLeggings, true);
        registerFallbackItem("warlock_boots", Material.NETHERITE_BOOTS, warlockBoots, true);
    }
    
    private void registerFallbackItem(String key, Material material, BaseItem item, boolean keepRecipe) {
        final NamespacedKey namespacedKey = championsKey(key);
        itemRegistry.registerFallbackItem(namespacedKey, material, item);
        if (keepRecipe) {
            final Recipe old = Bukkit.getRecipe(material.getKey());
            if (old == null) return;
            final CraftingRecipe craftingRecipe = adapter.convertToCustomRecipe(old);
            if (craftingRecipe != null) registry.registerRecipe(namespacedKey, craftingRecipe);
        }
    }

    private @NotNull NamespacedKey championsKey(@NotNull String key) {
        return new NamespacedKey(champions, key);
    }

}
