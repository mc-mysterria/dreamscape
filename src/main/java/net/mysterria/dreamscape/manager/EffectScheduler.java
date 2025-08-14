package net.mysterria.dreamscape.manager;

import net.mysterria.dreamscape.Dreamscape;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EffectScheduler implements Listener {

    private final Dreamscape plugin;
    private final DreamscapeManager manager;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> effectTask;

    private static final ZoneId GMT_PLUS_2 = ZoneId.of("GMT+2");

    public EffectScheduler(Dreamscape plugin, DreamscapeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void start() {
        stop();

        if (!manager.isActive() || manager.isDreamPhase()) return;

        effectTask = scheduler.scheduleAtFixedRate(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (manager.isActive() && !manager.isDreamPhase()) {
                    manager.applyEffects();
                }
            });
        }, 0, calculateInterval(), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (effectTask != null && !effectTask.isCancelled()) {
            effectTask.cancel(false);
            effectTask = null;
        }
    }

    private long calculateInterval() {
        if (manager.getTargetTime() == null) return 60000;

        Duration remaining = Duration.between(ZonedDateTime.now(GMT_PLUS_2), manager.getTargetTime());
        long hours = remaining.toHours();

        if (hours <= 1) {
            return 30 * 1000;
        } else if (hours <= 4) {
            return 5 * 60 * 1000;
        } else if (hours <= 8) {
            return 15 * 60 * 1000;
        } else if (hours <= 24) {
            return 30 * 60 * 1000;
        } else if (hours <= 48) {
            return 60 * 60 * 1000;
        } else {
            return 2 * 60 * 60 * 1000;
        }
    }

    public void reschedule() {
        start();
    }

    public void shutdown() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    @EventHandler
    public void onPlayerSleep(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            if (manager.isActive() && !manager.isDreamPhase()) {
                Player player = event.getPlayer();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isSleeping()) {
                        manager.grantSleepImmunity(player);
                    }
                }, 100L);
            }
        }
    }
}