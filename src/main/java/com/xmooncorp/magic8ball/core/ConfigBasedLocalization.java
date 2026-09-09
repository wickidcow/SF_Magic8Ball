package com.xmooncorp.magic8ball.core;

import org.bukkit.configuration.file.FileConfiguration;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ConfigBasedLocalization {

    private final FileConfiguration config;
    private final String name;
    private final String root;

    public ConfigBasedLocalization(@Nonnull String defaultLocalization, @Nonnull FileConfiguration config) {
        this.config = config;
        String lang = config.getString("options.lang");
        this.name = Objects.requireNonNullElse(lang, defaultLocalization);
        this.root = "languages." + this.name + ".";
    }

    @Nonnull
    public String getString(@Nonnull String path) {
        return Objects.requireNonNullElse(config.getString(root + path), "Locale Error: NotFound. Path: \"" + path + "\"");
    }

    @Nonnull
    public List<String> getStringList(@Nonnull String path) {
        List<String> values = config.getStringList(root + path);
        return values.isEmpty()
                ? Collections.singletonList("Locale Error: NotFound. Path: \"" + path + "\"")
                : values;
    }

    @Nonnull
    public String getName() {
        return name;
    }
}
