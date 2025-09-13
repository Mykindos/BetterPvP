package me.mykindos.betterpvp.core.combat.health;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.repo.HealthStat;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
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
            if (container.hasStat(HealthStat.class)) {
                health += container.getStat(HealthStat.class).orElseThrow().getValue();
            }
        }
        return health;
    }

    public double getMaxHealth(LivingEntity entity) {
        final EntityEquipment equipment = entity.getEquipment();
        Preconditions.checkNotNull(equipment, "Entity equipment cannot be null for entity: " + entity.getName());
        double additionalHealth = getHealth(equipment.getArmorContents());
        final double baseHealth = Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).getDefaultValue();
        return baseHealth + additionalHealth;
    }

    public double getHealth(LivingEntity entity) {
        return entity.getHealth();
    }

}
