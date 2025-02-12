package me.mykindos.betterpvp.core.logging.menu.button.type;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public interface IStringFilterValueButton extends IRefreshButton {
    @Override
    default void setRefreshing(boolean isRefreshing) {
    }

    void addValue(String context, String value);

    @Nullable String getSelected();

    void setSelectedContext(String newContext);

    HashMap<String, List<String>> getContextValues();
    /**
     * Set whether this button can be used
     * @param isStatic whether this button is static or not
     */
    void setStatic(boolean isStatic);
}
