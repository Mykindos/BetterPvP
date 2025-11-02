package me.mykindos.betterpvp.core.menu.button.filter;

public interface IFilterContext<Type> extends Comparable<IFilterContext<Type>>{
    /**
     * Get the string representation to show for this object
     * @return the element to display for this object
     */
    String getDisplay();
    Type getType();
}
