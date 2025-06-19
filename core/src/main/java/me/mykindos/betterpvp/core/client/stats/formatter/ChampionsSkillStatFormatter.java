package me.mykindos.betterpvp.core.client.stats.formatter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.ChampionsSkillStat;
import me.mykindos.betterpvp.core.client.stats.impl.MinecraftStat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class ChampionsSkillStatFormatter extends StatFormatter {

    public ChampionsSkillStatFormatter(ChampionsSkillStat stat) {
        super(stat);
    }

    @Inject
    public ChampionsSkillStatFormatter() {
        super();
    }

    @Override
    public @Nullable String getCategory() {
        return "Minecraft Stats";
    }

    @Override
    public String getStatType() {
        return this.getStat() == null ? MinecraftStat.prefix : super.getStatType();
    }


    @Override
    public Description getDescription(String statName, StatContainer statContainer, String period) {
        final ChampionsSkillStat championsSkillStat = getStat() == null ? ChampionsSkillStat.fromString(statName) : (ChampionsSkillStat) getStat();

        final List<Component> lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("An %s Minecraft Statistic", minecraftStat.getStatistic().getType().toString()));
        lore.add(Component.empty());
        lore.add(UtilMessage.deserialize("<white>Value</white>: <green>%s</green>", minecraftStat.getStat(statContainer, period)));

        Component name = Component.empty().append(Component.text(minecraftStat.getStatistic().toString(), NamedTextColor.WHITE));
        Material material = Material.GRASS_BLOCK;

        if (minecraftStat.getMaterial() != null) {
            name = name.appendSpace().append(Component.text(minecraftStat.getMaterial().toString(), NamedTextColor.GOLD));
            material = minecraftStat.getMaterial();
        } else if (minecraftStat.getEntityType() != null) {
            name = name.appendSpace().append(Component.text(minecraftStat.getEntityType().toString(), NamedTextColor.GOLD));
            material = Material.CREEPER_SPAWN_EGG;
        }

        final ItemView itemView = ItemView.builder()
                .displayName(name)
                .lore(lore)
                .frameLore(true)
                .material(material)
                .build();
        return Description.builder()
                .icon(itemView)
                .build();
    }


    /**
     * Get the {@link Material} associated with this stat if this {@link Statistic} is of type
     * {@link Statistic.Type#ITEM} or {@link Statistic.Type#BLOCK}
     * @param statName the statName to parse
     * @return the {@link Material} if the {@link Statistic#getType()} {@code =} {@link Statistic.Type#ITEM} or {@link Statistic.Type#BLOCK},
     * {@code null} otherwise
     */
    @Nullable
    protected Material getMaterial(String statName) {
        //format is as follows, MINECRAFT_STATISTIC__MATERIAL
        final String materialName = statName.substring(getStatType().length() + 2);
        return Material.getMaterial(materialName);
    }

    /**
     * Get the {@link EntityType} associated with this stat if this {@link Statistic} is of type
     * {@link Statistic.Type#ENTITY}
     * @param statName the statName to parse
     * @return the {@link EntityType} if the {@link Statistic#getType()} {@code =} {@link Statistic.Type#ENTITY},
     * {@code null} otherwise
     */
    @Nullable
    protected EntityType getEntityType(String statName) {
        //format is as follows, MINECRAFT_STATISTIC__ENTITY
        final String entityName = statName.substring(getStatType().length() + 2);
        return EntityType.fromName(entityName);
    }
}
