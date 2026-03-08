package me.mykindos.betterpvp.core.item.component.impl;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class ContainerComponent<T> extends AbstractItemComponent {
    
    @Getter
    protected @NotNull List<@NotNull T> container = List.of();

    protected ContainerComponent(String key) {
        super(key);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ContainerComponent<?> that = (ContainerComponent<?>) o;
        return container.equals(that.container);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        // the container hashcode is not the same for lists with the same elements, despite being equal
        // so we sum the hashcodes of each object in the container
        result = 31 * result + container.stream().mapToInt(T::hashCode).sum();
        return result;
    }
}
