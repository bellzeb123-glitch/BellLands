package pl.bell.lands.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import pl.bell.lands.BellLands;
import pl.bell.lands.config.LangManager;
import pl.bell.lands.integration.Pl3xMapHook;

import java.util.List;
import java.util.stream.Collectors;

public class BellLandsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LangManager lang = BellLands.getInstance().getLangManager();

        if (args.length == 0) {
            sender.sendMessage(lang.componentRaw("belllands-help-header",
                "version", BellLands.getInstance().getDescription().getVersion()));
            sender.sendMessage(lang.componentRaw("belllands-help-language"));
            sender.sendMessage(lang.componentRaw("belllands-help-reload"));
            sender.sendMessage(lang.componentRaw("belllands-help-cleanup"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("belllands.admin")) {
                sender.sendMessage(lang.component("reload-no-permission"));
                return true;
            }
            BellLands.getInstance().reload();
            sender.sendMessage(lang.component("reload-success"));
            return true;
        }

        if (args[0].equalsIgnoreCase("cleanup-duplicates")) {
            if (!sender.hasPermission("belllands.admin")) {
                sender.sendMessage(lang.component("reload-no-permission"));
                return true;
            }
            boolean confirm = args.length >= 2 && args[1].equalsIgnoreCase("confirm");
            var lm = BellLands.getInstance().getLandManager();
            if (!confirm) {
                int n = lm.cleanupCrossWorldDuplicates(true);
                sender.sendMessage(lang.component("cleanup-duplicates-preview", "count", n));
                return true;
            }
            int n = lm.cleanupCrossWorldDuplicates(false);
            Pl3xMapHook.drawAll();
            sender.sendMessage(lang.component("cleanup-duplicates-done", "count", n));
            return true;
        }

        if (args[0].equalsIgnoreCase("language") || args[0].equalsIgnoreCase("lang")) {
            if (!sender.hasPermission("belllands.admin")) {
                sender.sendMessage(lang.component("reload-no-permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(lang.component("language-usage"));
                return true;
            }
            String langCode = args[1].toLowerCase();
            if (!langCode.equals("en") && !langCode.equals("pl")) {
                sender.sendMessage(lang.component("language-invalid"));
                return true;
            }

            BellLands.getInstance().getConfig().set("language", langCode);
            BellLands.getInstance().saveConfig();
            BellLands.getInstance().reload();

            sender.sendMessage(BellLands.getInstance().getLangManager()
                .component("language-changed", "lang", langCode.toUpperCase()));
            return true;
        }

        sender.sendMessage(lang.componentRaw("belllands-help-header",
            "version", BellLands.getInstance().getDescription().getVersion()));
        sender.sendMessage(lang.componentRaw("belllands-help-language"));
        sender.sendMessage(lang.componentRaw("belllands-help-reload"));
        sender.sendMessage(lang.componentRaw("belllands-help-cleanup"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new java.util.ArrayList<>();
            if (sender.hasPermission("belllands.admin")) {
                options.add("language");
                options.add("reload");
                options.add("cleanup-duplicates");
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("language") || args[0].equalsIgnoreCase("lang"))) {
            if (!sender.hasPermission("belllands.admin")) {
                return List.of();
            }
            return filter(List.of("en", "pl"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("cleanup-duplicates")) {
            if (!sender.hasPermission("belllands.admin")) {
                return List.of();
            }
            return filter(List.of("confirm"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.startsWith(lower)).collect(Collectors.toList());
    }
}
