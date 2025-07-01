package me.mykindos.betterpvp.core.client.stats.formatter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.champions.ChampionsSkillStat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
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
        return "Champions Skill Stats";
    }

    @Override
    public String getStatType() {
        return this.getStat() == null ? ChampionsSkillStat.PREFIX : super.getStatType();
    }


    @Override
    public Description getDescription(String statName, StatContainer statContainer, String period) {
        final ChampionsSkillStat championsSkillStat = getStat() == null ? ChampionsSkillStat.fromString(statName) : (ChampionsSkillStat) getStat();

        final List<Component> lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("<white>Value</white>: <green>%s</green>", championsSkillStat.getStat(statContainer, period)));

        Component name = Component.empty().append(Component.text(championsSkillStat.getAction().name(), NamedTextColor.GREEN));

        if (championsSkillStat.getSkillName() != null) {
            name = name.appendSpace().append(Component.text(championsSkillStat.getSkillName(), NamedTextColor.GOLD));
        }
        if (championsSkillStat.getLevel() != -1) {
            name = name.appendSpace().append(Component.text(championsSkillStat.getLevel(), NamedTextColor.GOLD));
        }

        final ItemView itemView = ItemView.builder()
                .displayName(name)
                .lore(lore)
                .material(championsSkillStat.getAction() == ChampionsSkillStat.Action.EQUIP ? Material.SADDLE : Material.BLAZE_ROD)
                .frameLore(true)
                .build();
        return Description.builder()
                .icon(itemView)
                .build();
    }
}
