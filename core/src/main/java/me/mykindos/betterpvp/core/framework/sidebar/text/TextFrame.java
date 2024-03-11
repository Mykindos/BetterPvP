package me.mykindos.betterpvp.core.framework.sidebar.text;

import lombok.Value;

@Value(staticConstructor = "of")
public class TextFrame {

    String text;
    long delay;
}
