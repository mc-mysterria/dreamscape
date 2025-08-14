package net.mysterria.dreamscape.locale;


import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import net.mysterria.dreamscape.Dreamscape;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;

public class TranslationManager {

    private final Dreamscape plugin;
    private TranslationRegistry registry;

    public TranslationManager(Dreamscape plugin) {
        this.plugin = plugin;
    }

    public void load() {
        registry = TranslationRegistry.create(Key.key("dreamscape", "translations"));
        registry.defaultLocale(Locale.ENGLISH);

        saveDefaultTranslations();

        loadLocale(Locale.ENGLISH, "messages_en");
        loadLocale(new Locale("uk"), "messages_uk");

        GlobalTranslator.translator().addSource(registry);
    }

    private void loadLocale(Locale locale, String bundleName) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(
                    "dreamscape." + bundleName,
                    locale,
                    plugin.getClass().getClassLoader(),
                    UTF8ResourceBundleControl.get()
            );

            registry.registerAll(locale, bundle, false);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load translations for " + locale + ": " + e.getMessage());
        }
    }

    private void saveDefaultTranslations() {
        saveResource("messages_en.properties");
        saveResource("messages_uk.properties");
    }

    private void saveResource(String resourceName) {
        File file = new File(plugin.getDataFolder(), resourceName);
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getClass().getResourceAsStream("/dreamscape/" + resourceName)) {
                if (in != null) {
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        in.transferTo(out);
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save " + resourceName + ": " + e.getMessage());
            }
        }
    }

    public Component translate(String key, Locale locale, Component... args) {
        return Component.translatable(key, args);
    }
}