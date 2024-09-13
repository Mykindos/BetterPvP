package me.mykindos.betterpvp.core.logging.menu.button.type;

import org.jetbrains.annotations.Nullable;

public interface IStringFilterValueButton extends IRefreshButton {
    void addValue(String context, String value);

    @Nullable String getSelected();

    void setSelectedContext(String newContext);

    java.util.HashMap<String, java.util.List<String>> getContextValues();
}
