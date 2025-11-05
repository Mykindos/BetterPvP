package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.bloomrot.Bloomrot;
import me.mykindos.betterpvp.champions.item.ivybolt.Ivybolt;
import me.mykindos.betterpvp.champions.item.scythe.ScytheOfTheFallenLord;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemBootstrap;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.minecraft.MinecraftCraftingRecipeAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Singleton
public class ChampionsItemBoostrap implements ItemBootstrap {

    private boolean registered = false;

    @Inject private Core core;
    @Inject private ItemRegistry itemRegistry;
    @Inject private Champions champions;
    @Inject private CraftingRecipeRegistry craftingRecipeRegistry;
    @Inject private MinecraftCraftingRecipeAdapter adapter;
    @Inject private EnergyApple energyApple;
    @Inject private EnergyElixir energyElixir;
    @Inject private MushroomStew mushroomStew;
    @Inject private PurificationPotion purificationPotion;
    @Inject private RabbitStew rabbitStew;
    @Inject private SuspiciousStew suspiciousStew;
    @Inject private ThrowingWeb throwingWeb;
    @Inject private AncientSword ancientSword;
    @Inject private AncientAxe ancientAxe;
    @Inject private PowerSword powerSword;
    @Inject private PowerAxe powerAxe;
    @Inject private BoosterSword boosterSword;
    @Inject private BoosterAxe boosterAxe;
    @Inject private StandardSword standardSword;
    @Inject private StandardAxe standardAxe;
    @Inject private MagneticMaul magneticMaul;
    @Inject private GiantsBroadsword giantsBroadsword;
    @Inject private HyperAxe hyperAxe;
    @Inject private WindBlade windBlade;
    @Inject private MeridianScepter meridianScepter;
    @Inject private ScytheOfTheFallenLord scythe;
    @Inject private ThunderclapAegis thunderclapAegis;
    @Inject private AlligatorsTooth alligatorsTooth;
    @Inject private Rake rake;
    @Inject private RunedPickaxe runedPickaxe;
    @Inject private Bloomrot bloomrot;
    @Inject private Ivybolt ivybolt;
    @Inject private Mjolnir mjolnir;
    @Inject private AssassinHelmet assassinHelmet;
    @Inject private AssassinChestplate assassinChestplate;
    @Inject private AssassinLeggings assassinLeggings;
    @Inject private AssassinBoots assassinBoots;
    @Inject private BruteHelmet bruteHelmet;
    @Inject private BruteChestplate bruteChestplate;
    @Inject private BruteLeggings bruteLeggings;
    @Inject private BruteBoots bruteBoots;
    @Inject private KnightHelmet knightHelmet;
    @Inject private KnightChestplate knightChestplate;
    @Inject private KnightLeggings knightLeggings;
    @Inject private KnightBoots knightBoots;
    @Inject private MageHelmet mageHelmet;
    @Inject private MageChestplate mageChestplate;
    @Inject private MageLeggings mageLeggings;
    @Inject private MageBoots mageBoots;
    @Inject private RangerHelmet rangerHelmet;
    @Inject private RangerChestplate rangerChestplate;
    @Inject private RangerLeggings rangerLeggings;
    @Inject private RangerBoots rangerBoots;
    @Inject private WarlockHelmet warlockHelmet;
    @Inject private WarlockChestplate warlockChestplate;
    @Inject private WarlockLeggings warlockLeggings;
    @Inject private WarlockBoots warlockBoots;

    private @NotNull NamespacedKey championsKey(@NotNull String key) {
        return new NamespacedKey(champions, key);
    }

    @Inject
    @Override
    public void registerItems() {
        if (registered) return;
        registered = true;

        // Override default weapons so we can have the
        registerFallbackItem(itemRegistry, new NamespacedKey(core, "ancient_sword"), Material.NETHERITE_SWORD, ancientSword, false);
        registerFallbackItem(itemRegistry, new NamespacedKey(core, "ancient_axe"), Material.NETHERITE_AXE, ancientAxe, false);
        registerFallbackItem(itemRegistry, new NamespacedKey(core, "power_sword"), Material.DIAMOND_SWORD, powerSword, false);
        registerFallbackItem(itemRegistry, new NamespacedKey(core, "power_axe"), Material.DIAMOND_AXE, powerAxe, false);
        registerFallbackItem(itemRegistry, new NamespacedKey(core, "booster_sword"), Material.GOLDEN_SWORD, boosterSword, false);
        registerFallbackItem(itemRegistry, new NamespacedKey(core, "booster_axe"), Material.GOLDEN_AXE, boosterAxe, false);
        registerFallbackItem(itemRegistry, new NamespacedKey(core, "standard_sword"), Material.IRON_SWORD, standardSword, true);
        registerFallbackItem(itemRegistry, new NamespacedKey(core, "standard_axe"), Material.IRON_AXE, standardAxe, true);
        
        // Consumables
        registerFallbackItem(itemRegistry, "energy_apple", Material.APPLE, energyApple, false);
        registerFallbackItem(itemRegistry, "energy_elixir", Material.POTION, energyElixir, false);
        registerFallbackItem(itemRegistry, "mushroom_stew", Material.MUSHROOM_STEW, mushroomStew, false);
        itemRegistry.registerItem(championsKey("purification_potion"), purificationPotion);
        registerFallbackItem(itemRegistry, "rabbit_stew", Material.RABBIT_STEW, rabbitStew, false);
        registerFallbackItem(itemRegistry, "suspicious_stew", Material.SUSPICIOUS_STEW, suspiciousStew, false);
        registerFallbackItem(itemRegistry, "throwing_web", Material.COBWEB, throwingWeb, false);
        
        // Specials
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
        itemRegistry.registerItem(championsKey("mjolnir"), mjolnir);
        
        // Kits
        registerFallbackItem(itemRegistry, "assassin_helmet", Material.LEATHER_HELMET, assassinHelmet, true);
        registerFallbackItem(itemRegistry, "assassin_chestplate", Material.LEATHER_CHESTPLATE, assassinChestplate, true);
        registerFallbackItem(itemRegistry, "assassin_leggings", Material.LEATHER_LEGGINGS, assassinLeggings, true);
        registerFallbackItem(itemRegistry, "assassin_boots", Material.LEATHER_BOOTS, assassinBoots, true);
        registerFallbackItem(itemRegistry, "brute_helmet", Material.DIAMOND_HELMET, bruteHelmet, true);
        registerFallbackItem(itemRegistry, "brute_chestplate", Material.DIAMOND_CHESTPLATE, bruteChestplate, true);
        registerFallbackItem(itemRegistry, "brute_leggings", Material.DIAMOND_LEGGINGS, bruteLeggings, true);
        registerFallbackItem(itemRegistry, "brute_boots", Material.DIAMOND_BOOTS, bruteBoots, true);
        registerFallbackItem(itemRegistry, "knight_helmet", Material.IRON_HELMET, knightHelmet, true);
        registerFallbackItem(itemRegistry, "knight_chestplate", Material.IRON_CHESTPLATE, knightChestplate, true);
        registerFallbackItem(itemRegistry, "knight_leggings", Material.IRON_LEGGINGS, knightLeggings, true);
        registerFallbackItem(itemRegistry, "knight_boots", Material.IRON_BOOTS, knightBoots, true);
        registerFallbackItem(itemRegistry, "mage_helmet", Material.GOLDEN_HELMET, mageHelmet, true);
        registerFallbackItem(itemRegistry, "mage_chestplate", Material.GOLDEN_CHESTPLATE, mageChestplate, true);
        registerFallbackItem(itemRegistry, "mage_leggings", Material.GOLDEN_LEGGINGS, mageLeggings, true);
        registerFallbackItem(itemRegistry, "mage_boots", Material.GOLDEN_BOOTS, mageBoots, true);
        registerFallbackItem(itemRegistry, "ranger_helmet", Material.CHAINMAIL_HELMET, rangerHelmet, false);
        registerFallbackItem(itemRegistry, "ranger_chestplate", Material.CHAINMAIL_CHESTPLATE, rangerChestplate, false);
        registerFallbackItem(itemRegistry, "ranger_leggings", Material.CHAINMAIL_LEGGINGS, rangerLeggings, false);
        registerFallbackItem(itemRegistry, "ranger_boots", Material.CHAINMAIL_BOOTS, rangerBoots, false);
        registerFallbackItem(itemRegistry, "warlock_helmet", Material.NETHERITE_HELMET, warlockHelmet, true);
        registerFallbackItem(itemRegistry, "warlock_chestplate", Material.NETHERITE_CHESTPLATE, warlockChestplate, true);
        registerFallbackItem(itemRegistry, "warlock_leggings", Material.NETHERITE_LEGGINGS, warlockLeggings, true);
        registerFallbackItem(itemRegistry, "warlock_boots", Material.NETHERITE_BOOTS, warlockBoots, true);
    }

    private void registerFallbackItem(ItemRegistry registry, String key, Material material, BaseItem item, boolean keepRecipe) {
        final NamespacedKey namespacedKey = championsKey(key);
        registerFallbackItem(registry, namespacedKey, material, item, keepRecipe);
    }

    private void registerFallbackItem(ItemRegistry registry, NamespacedKey namespacedKey, Material material, BaseItem item, boolean keepRecipe) {
        registry.registerFallbackItem(namespacedKey, material, item);
        final List<Recipe> old = Bukkit.getRecipesFor(ItemStack.of(material));
        if (old.isEmpty()) {
            return;
        }

        final Map<NamespacedKey, CraftingRecipe> disabled = adapter.disableRecipesFor(material);
        if (keepRecipe) {
            for (Map.Entry<NamespacedKey, CraftingRecipe> entry : disabled.entrySet()) {
                craftingRecipeRegistry.registerRecipe(entry.getKey(), entry.getValue());
            }
        }
    }
}
