package me.mykindos.betterpvp.core.client.stats.formatter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.core.EffectDurationStat;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class EffectDurationStatFormatter extends StatFormatter {

    public EffectDurationStatFormatter(EffectDurationStat stat) {
        super(stat);
    }

    @Inject
    public EffectDurationStatFormatter() {
        super();
    }

    @Override
    public String getStatType() {
        return this.getStat() == null ? EffectDurationStat.PREFIX : super.getStatType();
    }


    @Override
    public Description getDescription(String statName, StatContainer statContainer, String period) {
        final EffectDurationStat effectDurationStat = getStat() == null ? EffectDurationStat.fromString(statName) : (EffectDurationStat) getStat();

        Duration duration = Duration.of(effectDurationStat.getStat(statContainer, period).longValue(), ChronoUnit.MILLIS);
        final List<Component> lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("<white>Value</white>: <green>%s</green>", UtilTime.humanReadableFormat(duration)));

        Component name = Component.empty().append(Component.text(effectDurationStat.getEffectType(), NamedTextColor.WHITE)).appendSpace()
                .append(Component.text(UtilFormat.cleanString(effectDurationStat.getRelation().name()), NamedTextColor.RED));
        if (!effectDurationStat.getEffectName().isEmpty()) {
            name = name.appendSpace().append(UtilMessage.deserialize("<gold>%s</gold>", effectDurationStat.getEffectName()));
        }

        //todo color potion based on Minecraft effect if present

        final ItemView itemView = ItemView.builder()
                .flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .displayName(name)
                .lore(lore)
                .frameLore(true)
                .material(Material.SPLASH_POTION)
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
