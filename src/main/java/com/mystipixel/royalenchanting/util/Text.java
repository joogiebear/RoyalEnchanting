package com.mystipixel.royalenchanting.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/** Legacy '&' colour strings (with &#RRGGBB hex) to Adventure components. */
public final class Text {

    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();

    private Text() {
    }

    public static Component chat(String input) {
        return AMP.deserialize(input == null ? "" : input);
    }

    /** For item names and lore: the client italicises those unless told not to. */
    public static Component item(String input) {
        return chat(input).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static List<Component> items(List<String> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(item(line));
        }
        return out;
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
