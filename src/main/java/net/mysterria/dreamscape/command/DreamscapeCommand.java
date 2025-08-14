package net.mysterria.dreamscape.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.mysterria.dreamscape.Dreamscape;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public class DreamscapeCommand {

    private final Dreamscape plugin;
    private static final ZoneId GMT_PLUS_2 = ZoneId.of("GMT+2");

    public DreamscapeCommand(Dreamscape plugin) {
        this.plugin = plugin;
    }

    public void register(Commands commands) {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("dreamscape")
                .requires(sender -> sender.getSender().hasPermission("dreamscape.admin"))
                .then(Commands.argument("day", StringArgumentType.word())
                        .then(Commands.argument("hour", IntegerArgumentType.integer(0, 23))
                                .executes(ctx -> {
                                    String day = StringArgumentType.getString(ctx, "day");
                                    int hour = ctx.getArgument("hour", Integer.class);
                                    return executeStart(ctx.getSource().getSender(), day, hour);
                                })
                        )
                )
                .then(Commands.literal("lift")
                        .executes(ctx -> executeLift(ctx.getSource().getSender()))
                )
                .then(Commands.literal("status")
                        .executes(ctx -> executeStatus(ctx.getSource().getSender()))
                )
                .build();

        commands.register(root, "Main dreamscape command");
    }

    private int executeStart(CommandSender sender, String dayStr, int hour) {
        DayOfWeek targetDay = parseDayOfWeek(dayStr);
        if (targetDay == null) {
            Component msg = plugin.getTranslationManager().translate(
                    "dreamscape.command.invalid_day",
                    getLocale(sender),
                    Component.text(dayStr)
            );
            sender.sendMessage(msg);
            return 0;
        }

        ZonedDateTime now = ZonedDateTime.now(GMT_PLUS_2);
        ZonedDateTime target = now.with(TemporalAdjusters.nextOrSame(targetDay))
                .with(LocalTime.of(hour, 0));

        if (target.isBefore(now) || target.isEqual(now)) {
            target = target.plusWeeks(1);
        }

        plugin.getDreamscapeManager().startCountdown(target);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy HH:mm")
                .withZone(GMT_PLUS_2);

        Component msg = plugin.getTranslationManager().translate(
                "dreamscape.command.started",
                getLocale(sender),
                Component.text(formatter.format(target))
        );
        sender.sendMessage(msg);

        Component broadcast = plugin.getTranslationManager().translate(
                "dreamscape.broadcast.started",
                Locale.ENGLISH,
                Component.text(formatter.format(target))
        );
        plugin.getServer().broadcast(broadcast);

        return Command.SINGLE_SUCCESS;
    }

    private int executeLift(CommandSender sender) {
        if (!plugin.getDreamscapeManager().isActive()) {
            Component msg = plugin.getTranslationManager().translate(
                    "dreamscape.command.not_active",
                    getLocale(sender)
            );
            sender.sendMessage(msg);
            return 0;
        }

        plugin.getDreamscapeManager().liftDreamscape();

        Component msg = plugin.getTranslationManager().translate(
                "dreamscape.command.lifted",
                getLocale(sender)
        );
        sender.sendMessage(msg);

        Component broadcast = plugin.getTranslationManager().translate(
                "dreamscape.broadcast.lifted",
                Locale.ENGLISH
        );
        plugin.getServer().broadcast(broadcast);

        return Command.SINGLE_SUCCESS;
    }

    private int executeStatus(CommandSender sender) {
        if (!plugin.getDreamscapeManager().isActive()) {
            Component msg = plugin.getTranslationManager().translate(
                    "dreamscape.status.inactive",
                    getLocale(sender)
            );
            sender.sendMessage(msg);
            return Command.SINGLE_SUCCESS;
        }

        ZonedDateTime target = plugin.getDreamscapeManager().getTargetTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy HH:mm")
                .withZone(GMT_PLUS_2);

        Component msg;
        if (plugin.getDreamscapeManager().isDreamPhase()) {
            msg = plugin.getTranslationManager().translate(
                    "dreamscape.status.dream_phase",
                    getLocale(sender)
            );
        } else {
            msg = plugin.getTranslationManager().translate(
                    "dreamscape.status.countdown",
                    getLocale(sender),
                    Component.text(formatter.format(target))
            );
        }

        sender.sendMessage(msg);
        return Command.SINGLE_SUCCESS;
    }

    private DayOfWeek parseDayOfWeek(String day) {
        String lower = day.toLowerCase();
        return switch (lower) {
            case "monday", "mon" -> DayOfWeek.MONDAY;
            case "tuesday", "tue" -> DayOfWeek.TUESDAY;
            case "wednesday", "wed" -> DayOfWeek.WEDNESDAY;
            case "thursday", "thu" -> DayOfWeek.THURSDAY;
            case "friday", "fri" -> DayOfWeek.FRIDAY;
            case "saturday", "sat" -> DayOfWeek.SATURDAY;
            case "sunday", "sun" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private Locale getLocale(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.locale();
        }
        return Locale.ENGLISH;
    }
}