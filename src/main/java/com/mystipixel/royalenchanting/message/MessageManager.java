package com.mystipixel.royalenchanting.message;

import com.mystipixel.royalenchanting.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Player-facing text from {@code messages.yml}, including every GUI name and lore line. The copy in the
 * jar backs every key, so a server file missing a key (after an update) still shows sensible text; blank
 * text silences a chat message entirely.
 */
public final class MessageManager {

    private final JavaPlugin plugin;
    private YamlConfiguration messages;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in != null) {
                loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (java.io.IOException ignored) {
            // Defaults are a convenience; the server file alone still works.
        }
        this.messages = loaded;
    }

    /** One line, with {@code %key%} replacements given as alternating key/value pairs (keys without %). */
    public String get(String key, String... replacements) {
        return replace(messages.getString(key), replacements);
    }

    public List<String> list(String key, String... replacements) {
        List<String> out = new ArrayList<>();
        for (String line : messages.getStringList(key)) {
            out.add(replace(line, replacements));
        }
        return out;
    }

    public void send(CommandSender target, String key, String... replacements) {
        String raw = get(key, replacements);
        if (raw == null || raw.isBlank()) {
            return;
        }
        String prefix = messages.getString("prefix");
        target.sendMessage(Text.chat((prefix == null ? "" : prefix) + raw));
    }

    private static String replace(String raw, String... replacements) {
        if (raw == null) {
            return "";
        }
        String out = raw;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            out = out.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return out;
    }
}
