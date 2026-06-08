package me.mykindos.betterpvp.progression.profession.skill.woodcutting.sapling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.EnumMap;
import java.util.Map;

@BPvPListener
@Singleton
public class SaplingSkillListener implements Listener {

    private final Map<Material, SaplingSkill> saplingSkills = new EnumMap<>(Material.class);

    @Inject
    private SaplingSkillListener(AcaciaSaplingSkill acaciaSaplingSkill,
                                BirchSaplingSkill birchSaplingSkill,
                                CherrySaplingSkill cherrySaplingSkill,
                                DarkOakSaplingSkill darkOakSaplingSkill,
                                JungleSaplingSkill jungleSaplingSkill,
                                MangroveSaplingSkill mangroveSaplingSkill,
                                PaleOakSaplingSkill paleOakSaplingSkill,
                                SpruceSaplingSkill spruceSaplingSkill) {
        register(acaciaSaplingSkill);
        register(birchSaplingSkill);
        register(cherrySaplingSkill);
        register(darkOakSaplingSkill);
        register(jungleSaplingSkill);
        register(mangroveSaplingSkill);
        register(paleOakSaplingSkill);
        register(spruceSaplingSkill);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlantSapling(BlockPlaceEvent event) {
        SaplingSkill skill = saplingSkills.get(event.getBlockPlaced().getType());
        if (skill == null) return;
        if (skill.getSkillLevel(event.getPlayer()) > 0) return;

        event.setCancelled(true);
        UtilMessage.message(event.getPlayer(), "core.prefix.woodcutting",
                "progression.woodcutting.sapling.unlock-required",
                Component.text(skill.getTreeName(), NamedTextColor.GREEN));
    }

    private void register(SaplingSkill skill) {
        saplingSkills.put(skill.getSaplingMaterial(), skill);
    }
}
