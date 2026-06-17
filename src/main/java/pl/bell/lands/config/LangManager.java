package pl.bell.lands.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.bell.lands.BellLands;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LangManager {

    private final BellLands plugin;
    private FileConfiguration lang;

    public LangManager(BellLands plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String language = plugin.getConfig().getString("language", "en");
        lang = loadAndMerge(language);
    }

    private FileConfiguration loadAndMerge(String langCode) {
        String fileName = "lang/" + langCode + ".yml";
        File diskFile = new File(plugin.getDataFolder(), fileName);

        FileConfiguration base = loadFromJar(fileName);
        if (base == null) {
            plugin.getLogger().warning("Lang file not found in jar: " + fileName + ", falling back to en");
            base = loadFromJar("lang/en.yml");
        }
        if (base == null) {
            plugin.getLogger().severe("No lang files in jar! Loading from disk.");
            return diskFile.exists()
                ? YamlConfiguration.loadConfiguration(diskFile)
                : new YamlConfiguration();
        }

        if (diskFile.exists()) {
            FileConfiguration disk = YamlConfiguration.loadConfiguration(diskFile);
            for (String key : disk.getKeys(true)) {
                if (!disk.isConfigurationSection(key) && base.contains(key)) {
                    base.set(key, disk.get(key));
                }
            }
        }

        try {
            diskFile.getParentFile().mkdirs();
            base.save(diskFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save lang file: " + fileName, e);
        }

        return base;
    }

    private FileConfiguration loadFromJar(String fileName) {
        try (InputStream stream = plugin.getResource(fileName)) {
            if (stream == null) return null;
            return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading lang from jar: " + fileName, e);
            return null;
        }
    }

    public String get(String key, Object... args) {
        String prefix = lang.getString("prefix", "&8[&6BellLands&8] &r");
        String msg = lang.getString(key, "&cMissing lang key: " + key);
        msg = prefix + msg;
        return applyPlaceholders(msg, args);
    }

    public String getRaw(String key, Object... args) {
        String msg = lang.getString(key, "&cMissing lang key: " + key);
        return applyPlaceholders(msg, args);
    }

    public List<String> getList(String key, Object... args) {
        List<String> list = lang.getStringList(key);
        return list.stream()
            .map(line -> applyPlaceholders(line, args))
            .collect(Collectors.toList());
    }

    public Component component(String key, Object... args) {
        return colorize(get(key, args));
    }

    public Component componentRaw(String key, Object... args) {
        return colorize(getRaw(key, args));
    }

    public Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    private String applyPlaceholders(String msg, Object... args) {
        for (int i = 0; i + 1 < args.length; i += 2) {
            String placeholder = "{" + args[i] + "}";
            String value = String.valueOf(args[i + 1]);
            msg = msg.replace(placeholder, value);
        }
        return msg;
    }
}
