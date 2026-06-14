package pl.bell.lands.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.bell.lands.BellLands;
import pl.bell.lands.model.Land;
import pl.bell.lands.manager.LandManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClaimTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
        "unclaim", "trust", "untrust", "flag", "flags", "info", "gui", "help"
    );

    private static final List<String> BOOLEAN_VALUES = List.of("true", "false");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            return filterStartsWith(SUBCOMMANDS, args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            switch (sub) {
                case "trust" -> {
                    return filterStartsWith(getOnlinePlayerNames(player), args[1]);
                }
                case "untrust" -> {
                    return filterStartsWith(getTrustedNames(player), args[1]);
                }
                case "flag" -> {
                    return filterStartsWith(Arrays.asList(Land.ALL_FLAGS), args[1]);
                }
            }
        }

        if (args.length == 3 && sub.equals("flag")) {
            return filterStartsWith(BOOLEAN_VALUES, args[2]);
        }

        return List.of();
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
            .filter(s -> s.toLowerCase().startsWith(lower))
            .collect(Collectors.toList());
    }

    private List<String> getOnlinePlayerNames(Player sender) {
        return Bukkit.getOnlinePlayers().stream()
            .filter(p -> !p.equals(sender))
            .map(Player::getName)
            .collect(Collectors.toList());
    }

    private List<String> getTrustedNames(Player player) {
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(player.getLocation().getChunk());
        if (opt.isEmpty()) return List.of();

        Land land = opt.get();
        List<String> names = new ArrayList<>();
        for (var uuid : land.getTrusted()) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name != null) names.add(name);
        }
        return names;
    }
}
