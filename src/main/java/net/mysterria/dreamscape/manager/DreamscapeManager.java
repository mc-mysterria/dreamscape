package net.mysterria.dreamscape.manager;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.mysterria.dreamscape.Dreamscape;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DreamscapeManager implements Listener {

    private final Dreamscape plugin;
    private ZonedDateTime targetTime;

    private boolean isActive;
    private boolean isDreamPhase;

    private final Map<UUID, Long> sleepImmunity = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> playerBossBars = new ConcurrentHashMap<>();

    private static final ZoneId GMT_PLUS_2 = ZoneId.of("GMT+2");
    private static final String IMMUNITY_PERMISSION = "dreamscape.immune";

    public DreamscapeManager(Dreamscape plugin) {
        this.plugin = plugin;
    }

    public void startCountdown(ZonedDateTime target) {
        this.targetTime = target;
        this.isActive = true;
        this.isDreamPhase = false;
        plugin.getEffectScheduler().start();
        saveState();
    }

    public void liftDreamscape() {
        this.isActive = false;
        this.isDreamPhase = false;
        this.targetTime = null;
        clearAllEffects();
        plugin.getEffectScheduler().stop();
        saveState();
    }

    public void applyEffects() {
        if (!isActive || isDreamPhase) return;

        ZonedDateTime now = ZonedDateTime.now(GMT_PLUS_2);
        Duration remaining = Duration.between(now, targetTime);

        if (remaining.isNegative() || remaining.isZero()) {
            enterDreamPhase();
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(IMMUNITY_PERMISSION)) continue;
            if (isImmune(player.getUniqueId())) continue;

            applyDreamEffects(player, remaining);

            if (remaining.toHours() <= 1) {
                showBossBar(player, remaining);
            }
        }
    }

    private void applyDreamEffects(Player player, Duration remaining) {
        long hours = remaining.toHours();
        int intensity = calculateIntensity(hours);

        if (hours <= 1) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, intensity));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
            if (Math.random() < 0.3) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            }
            playSpookySound(player);
        } else if (hours <= 8) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, intensity));
            if (Math.random() < 0.2) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0));
            }
        } else if (hours <= 24) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
        } else {
            if (Math.random() < 0.1) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
            }
        }

        sendDreamMessage(player, remaining);
    }

    private int calculateIntensity(long hours) {
        if (hours <= 1) return 2;
        if (hours <= 4) return 1;
        return 0;
    }

    private void playSpookySound(Player player) {
        String[] sounds = {
                "AMBIENT_CAVE",
                "AMBIENT_BASALT_DELTAS_MOOD",
                "AMBIENT_CRIMSON_FOREST_MOOD",
                "AMBIENT_NETHER_WASTES_MOOD",
                "AMBIENT_SOUL_SAND_VALLEY_MOOD"
        };
        String sound = sounds[new Random().nextInt(sounds.length)];
        player.playSound(player.getLocation(), sound, 0.5f, 0.8f);
    }

    private void sendDreamMessage(Player player, Duration remaining) {
        if (Math.random() < 0.15) {
            Component msg = plugin.getTranslationManager().translate(
                    "dreamscape.effect.message",
                    player.locale(),
                    Component.text(formatDuration(remaining))
            );
            player.sendMessage(msg);
        }
    }

    private void showBossBar(Player player, Duration remaining) {
        BossBar bar = playerBossBars.computeIfAbsent(player.getUniqueId(), k -> {
            BossBar newBar = BossBar.bossBar(
                    Component.empty(),
                    1.0f,
                    BossBar.Color.PURPLE,
                    BossBar.Overlay.PROGRESS
            );
            player.showBossBar(newBar);
            return newBar;
        });

        Component name = plugin.getTranslationManager().translate(
                "dreamscape.bossbar.countdown",
                player.locale(),
                Component.text(formatDuration(remaining))
        );

        float progress = Math.max(0, Math.min(1, remaining.toMinutes() / 60.0f));
        bar.name(name);
        bar.progress(progress);
    }

    private void enterDreamPhase() {
        isDreamPhase = true;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(IMMUNITY_PERMISSION)) continue;

            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));

            Component kickMsg = plugin.getTranslationManager().translate(
                    "dreamscape.kick.message",
                    player.locale()
            );

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.kick(kickMsg);
            }, 100L);
        }
    }

    public void grantSleepImmunity(Player player) {
        if (!isActive || isDreamPhase) return;

        Duration remaining = Duration.between(ZonedDateTime.now(GMT_PLUS_2), targetTime);
        long immunityDuration = calculateImmunityDuration(remaining);

        sleepImmunity.put(player.getUniqueId(), System.currentTimeMillis() + immunityDuration);

        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        Component msg = plugin.getTranslationManager().translate(
                "dreamscape.sleep.immunity",
                player.locale(),
                Component.text(immunityDuration / 60000)
        );
        player.sendMessage(msg);
    }

    private long calculateImmunityDuration(Duration remaining) {
        long hours = remaining.toHours();
        if (hours <= 1) return 5 * 60 * 1000;
        if (hours <= 8) return 30 * 60 * 1000;
        if (hours <= 24) return 2 * 60 * 60 * 1000;
        return 4 * 60 * 60 * 1000;
    }

    private boolean isImmune(UUID playerId) {
        Long immunityEnd = sleepImmunity.get(playerId);
        if (immunityEnd == null) return false;

        if (System.currentTimeMillis() >= immunityEnd) {
            sleepImmunity.remove(playerId);
            return false;
        }
        return true;
    }

    private void clearAllEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.NAUSEA);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.DARKNESS);

            BossBar bar = playerBossBars.remove(player.getUniqueId());
            if (bar != null) {
                player.hideBossBar(bar);
            }
        }
        sleepImmunity.clear();
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (isDreamPhase && !event.getPlayer().hasPermission(IMMUNITY_PERMISSION)) {
            Component msg = plugin.getTranslationManager().translate(
                    "dreamscape.login.denied",
                    event.getPlayer().locale()
            );
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, msg);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isActive && !isDreamPhase) {
            Duration remaining = Duration.between(ZonedDateTime.now(GMT_PLUS_2), targetTime);
            if (remaining.toHours() <= 1) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    showBossBar(event.getPlayer(), remaining);
                }, 20L);
            }
        }
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%dh %dm", hours, minutes);
    }

    public void saveState() {
        plugin.getStorage().saveState(this);
    }

    public void loadState() {
        plugin.getStorage().loadState(this);
        if (isActive && !isDreamPhase) {
            plugin.getEffectScheduler().start();
        }
    }

    public ZonedDateTime getTargetTime() {
        return targetTime;
    }

    public void setTargetTime(ZonedDateTime time) {
        this.targetTime = time;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public boolean isDreamPhase() {
        return isDreamPhase;
    }

    public void setDreamPhase(boolean phase) {
        this.isDreamPhase = phase;
    }
}