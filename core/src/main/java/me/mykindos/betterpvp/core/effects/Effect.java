package me.mykindos.betterpvp.core.effects;

import lombok.Data;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.builder.ItemBuilder;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.Predicate;

@Data
public class Effect {

    private final String uuid;
    private LivingEntity applier;
    private final EffectType effectType;
    private final String name;
    private long length;
    private long rawLength;
    private int amplifier;
    private final boolean permanent;
    private Predicate<LivingEntity> removalPredicate;


    public Effect(String uuid, LivingEntity applier, EffectType effectType, String name, int amplifier, long length, boolean permanent, Predicate<LivingEntity> removalPredicate) {
        this.uuid = uuid;
        this.applier = applier;
        this.effectType = effectType;
        this.name = name;
        this.rawLength = length + 50;
        this.length = System.currentTimeMillis() + length + 50;
        this.amplifier = amplifier;
        this.permanent = permanent;
        this.removalPredicate = removalPredicate;
    }

    public void setLength(long length) {
        this.rawLength = length + 50;
        this.length = System.currentTimeMillis() + length + 50;
    }

    public boolean hasExpired() {
        return rawLength >= 0 && length - System.currentTimeMillis() <= 0 && !permanent;
    }

    public long getRemainingDuration() {
        return length - System.currentTimeMillis();
    }

    public int getVanillaDuration() {
        return permanent ? -1 : (int) Math.ceil((rawLength / 1000d) * 20d);
    }

    public int getRemainingVanillaDuration() {
        return permanent ? -1 : (int) Math.ceil((getRemainingDuration() / 1000d) * 20d);
    }

    public Description getDescription() {
        List<Component> lore = List.of(
                UtilMessage.deserialize("<green>%s</green> remaining", UtilTime.getTime(getRemainingDuration(), 1)),
                UtilMessage.deserialize(getEffectType().getDescription(getAmplifier()))
        );

        ItemView itemView = ItemView.builder()
                .displayName(Component.text(getEffectType().getName() + " " + UtilFormat.getRomanNumeral(getAmplifier()), NamedTextColor.WHITE))
                .material(getEffectType().getMaterial())
                .customModelData(getEffectType().getModelData())
                .lore(lore)
                .build();

        return Description.builder().icon(itemView).build();
    }
}
