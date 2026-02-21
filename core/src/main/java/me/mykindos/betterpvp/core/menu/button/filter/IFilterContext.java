package me.mykindos.betterpvp.core.menu.button.filter;

public interface IFilterContext<T> extends Comparable<IFilterContext<T>>{
    /**
     * Get the string representation to show for this object
     * @return the element to display for this object
     */
    String getDisplay();
    T getType();
}
