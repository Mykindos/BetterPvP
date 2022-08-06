package me.mykindos.betterpvp.core.combat.armour;

import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class ArmourManager extends Manager<Armour> {

    private final ArmourRepository armourRepository;

    @Inject
    public ArmourManager(ArmourRepository armourRepository) {
        this.armourRepository = armourRepository;
        loadFromList(armourRepository.getAll());
    }

    @Override
    public void loadFromList(List<Armour> armourList) {
        objects.clear();
        armourList.forEach(armour -> objects.put(armour.getItemType().toUpperCase(), armour));
    }

    public void reload() {
        loadFromList(armourRepository.getAll());
    }

    public double getDamageReduced(double baseDamage, LivingEntity entity) {
        if (entity.getEquipment() == null) return baseDamage;
        ItemStack helmet = entity.getEquipment().getHelmet();
        ItemStack chest = entity.getEquipment().getChestplate();
        ItemStack pants = entity.getEquipment().getLeggings();
        ItemStack boots = entity.getEquipment().getBoots();
        double reduction = 0.0;

        if (helmet != null) {
            reduction += getReductionForItem(helmet);
        }

        if (boots != null) {
            reduction += getReductionForItem(boots);
        }

        if (pants != null) {
            reduction += getReductionForItem(pants);
        }

        if (chest != null) {
            reduction += getReductionForItem(chest);
        }
        if (reduction == 0) return baseDamage;

        return baseDamage * (100 - reduction) / 100;
    }

    public double getReductionForItem(ItemStack item) {
        double reduction = 0;

        Optional<Armour> armourOptional = getObject(item.getType().name());
        if (armourOptional.isPresent()) {
            reduction = armourOptional.get().getReduction();
        }
        return reduction;
    }
}
