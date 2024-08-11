package me.mykindos.betterpvp.progression.profession.woodcutting

import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.logging.CustomLogger
import me.mykindos.betterpvp.core.utilities.model.WeighedList
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import java.util.*


/**
 * - Loads the YAML configuration for the Woodcutting Profession & returns the loaded data
 * - This function will cause side effects by logging messages to the console
 */
fun loadConfigForWoodcutting(config: ExtendedYamlConfiguration,
                             log: CustomLogger): Pair<EnumMap<Material, Long>, WeighedList<WoodcuttingLootType>> {


    /**
     * This function will try to get a configuration section for the path but if there is none, it will create
     * a new section at path
     */
    fun createOrGetSection(parentSection: ConfigurationSection, path: String): ConfigurationSection {
        return parentSection.getConfigurationSection(path) ?: parentSection.createSection(path)
    }


    val woodcuttingSection: ConfigurationSection = createOrGetSection(config, "woodcutting")

    val experiencePerWood = EnumMap<Material, Long>(Material::class.java).apply {
        val experienceSection = createOrGetSection(woodcuttingSection, "experiencePerWood")

        for (materialAsKey: String in experienceSection.getKeys(false)) {
            val woodLogMaterial = Material.getMaterial(materialAsKey.uppercase()) ?: continue
            this[woodLogMaterial] = experienceSection.getLong(materialAsKey)
        }
    }.also { log.info("Loaded ${it.size} woodcutting blocks").submit() }

    val lootTypes = WeighedList<WoodcuttingLootType>().apply {

        val lootSection: ConfigurationSection = createOrGetSection(woodcuttingSection, "loot")

        for (lootItemKey: String in lootSection.getKeys(false)) {

            val lootItemSection = lootSection.getConfigurationSection(lootItemKey) ?: continue
            val itemMaterialAsString = lootItemSection.getString("material") ?: continue
            val material = Material.getMaterial(itemMaterialAsString.uppercase()) ?: continue

            lootItemSection.also {
                val customModelData = it.getInt("customModelData")
                val frequency = it.getInt("frequency")
                val minAmount = it.getInt("minAmount")
                val maxAmount = it.getInt("maxAmount")

                this.add(frequency, 1, WoodcuttingLootType(
                    material, customModelData, minAmount, maxAmount
                ))
            }

        }
    }.also { log.info("Loaded ${it.size()} woodcutting loot types").submit() }

    return experiencePerWood to lootTypes
}


/**
 * Represents a type of loot one can obtain from the *Woodcutting* profession
 */
data class WoodcuttingLootType(
    val material: Material, val customModelData: Int, val minAmount: Int, val maxAmount: Int
)
