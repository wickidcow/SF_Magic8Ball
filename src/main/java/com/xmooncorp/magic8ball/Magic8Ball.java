package com.xmooncorp.magic8ball;

import com.xmooncorp.magic8ball.core.ConfigBasedLocalization;
import com.xmooncorp.magic8ball.implementation.Items;
import com.xmooncorp.magic8ball.implementation.resources.Magic8BallFragmentResource;
import com.xmooncorp.magic8ball.implementation.setup.ItemSetup;
import com.xmooncorp.magic8ball.implementation.setup.ResearchSetup;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class Magic8Ball extends JavaPlugin implements SlimefunAddon {

    private static Magic8Ball instance;
    private ConfigBasedLocalization localization;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        config = getConfig();

        loadLanguage();

        log(localization().getString("console.registering-geo"));
        registerGeoResources();

        log(localization().getString("console.loading-items"));
        loadItems();

        log(localization().getString("console.loading-researches"));
        loadResearches();

        log(localization().getString("console.addon-enabled"));
    }

    @Override
    public void onDisable() {
        if (localization != null) {
            log(localization.getString("console.addon-disabled"));
        }
        instance = null;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/wickidcow/SF_Magic8Ball/issues";
    }

    @Nonnull
    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Nonnull
    public static Magic8Ball instance() {
        return instance;
    }

    public static void log(@Nonnull String message) {
        instance().getLogger().info(message);
    }

    private void loadItems() {
        try {
            ItemSetup.setup(this);
        } catch (Exception | LinkageError x) {
            getLogger().log(Level.SEVERE, x, () -> "Error loading Magic8Ball items");
        }
    }

    private void loadResearches() {
        try {
            ResearchSetup.setupResearches();
        } catch (Exception | LinkageError x) {
            getLogger().log(Level.SEVERE, x, () -> "Error loading Magic8Ball researches");
        }
    }

    private void registerGeoResources() {
        new Magic8BallFragmentResource(this, Items.MAGIC_8_BALL_FRAGMENT, "magic8ball_fragment_geo_resource").register();
    }

    private void loadLanguage() {
        localization = new ConfigBasedLocalization("en-US", config());
        log(localization.getString("console.loading-language"));
        log(localization.getString("console.loaded-language") + " " + localization.getName());
    }

    public ConfigBasedLocalization localization() {
        return localization;
    }

    public FileConfiguration config() {
        return config;
    }
}
