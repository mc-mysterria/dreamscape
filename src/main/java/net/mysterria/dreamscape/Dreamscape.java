package net.mysterria.dreamscape;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.mysterria.dreamscape.command.DreamscapeCommand;
import net.mysterria.dreamscape.locale.TranslationManager;
import net.mysterria.dreamscape.manager.DreamscapeManager;
import net.mysterria.dreamscape.manager.EffectScheduler;
import net.mysterria.dreamscape.storage.DreamscapeStorage;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Dreamscape extends JavaPlugin {
    private static Dreamscape instance;
    private DreamscapeManager dreamscapeManager;
    private EffectScheduler effectScheduler;
    private DreamscapeStorage storage;
    private TranslationManager translationManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        translationManager = new TranslationManager(this);
        translationManager.load();

        storage = new DreamscapeStorage(this);
        dreamscapeManager = new DreamscapeManager(this);
        effectScheduler = new EffectScheduler(this, dreamscapeManager);

        dreamscapeManager.loadState();

        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            new DreamscapeCommand(this).register(commands);
        });

        getServer().getPluginManager().registerEvents(dreamscapeManager, this);
    }

    @Override
    public void onDisable() {
        if (dreamscapeManager != null) {
            dreamscapeManager.saveState();
        }
        if (effectScheduler != null) {
            effectScheduler.shutdown();
        }
    }

    public static Dreamscape getInstance() {
        return instance;
    }

    public DreamscapeManager getDreamscapeManager() {
        return dreamscapeManager;
    }

    public EffectScheduler getEffectScheduler() {
        return effectScheduler;
    }

    public DreamscapeStorage getStorage() {
        return storage;
    }

    public TranslationManager getTranslationManager() {
        return translationManager;
    }
}
