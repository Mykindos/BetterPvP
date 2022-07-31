package me.mykindos.betterpvp.core.discord.embeds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EmbedField
{
    private String name;
    private String value;
    private boolean inline;

}