package me.mykindos.betterpvp.core.discord.embeds;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.awt.*;
import java.util.List;

@Builder
@Getter
public class EmbedObject
{
    private String title;
    private String description;
    private String url;
    private Color color;

    private EmbedFooter footer;
    private String thumbnail;
    private String image;
    private EmbedAuthor author;

    @Singular
    private List<EmbedField> fields;

}