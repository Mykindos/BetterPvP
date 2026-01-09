package me.mykindos.betterpvp.core.combat.health;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypes;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class EntityHealthService {

    private final ItemFactory itemFactory;

    @Inject
    private EntityHealthService(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    public double getHealth(ItemStack[] healthItems) {
        final List<ItemInstance> itemInstances = itemFactory.fromArray(healthItems);
        double health = 0;
        for (ItemInstance itemInstance : itemInstances) {
            if (itemInstance == null) {
                continue;
            }

            final Optional<StatContainerComponent> componentOpt = itemInstance.getComponent(StatContainerComponent.class);
            if (componentOpt.isEmpty()) {
                continue;
            }

            final StatContainerComponent container = componentOpt.get();
            if (container.hasStat(StatTypes.HEALTH)) {
                health += container.getStat(StatTypes.HEALTH).orElseThrow().getValue();
            }
        }
        return health;
    }

    public void setBaseHealth(LivingEntity entity, double baseHealth) {
        final AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        Preconditions.checkNotNull(attribute, "Entity does not have a max health attribute: " + entity.getName());
        attribute.setBaseValue(baseHealth);
    }

    public void resetBaseHealth(LivingEntity entity) {
        final AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        Preconditions.checkNotNull(attribute, "Entity does not have a max health attribute: " + entity.getName());
        attribute.setBaseValue(attribute.getDefaultValue());
    }

    public double getMaxHealth(LivingEntity entity) {
        final double baseHealth = Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();
        double additionalHealth = 0;

        final EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) {
            additionalHealth += getHealth(equipment.getArmorContents());
        }

        return baseHealth + additionalHealth;
    }

    public double getHealth(LivingEntity entity) {
        return entity.getHealth();
    }

}
