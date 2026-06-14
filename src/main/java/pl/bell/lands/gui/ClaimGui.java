package pl.bell.lands.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import pl.bell.lands.BellLands;
import pl.bell.lands.config.LangManager;
import pl.bell.lands.model.Land;

import java.util.*;

public class ClaimGui {

    private static final Material[] FLAG_MATERIALS = {
        Material.IRON_SWORD,        // pvp
        Material.TNT,               // explosions
        Material.FLINT_AND_STEEL,   // fire-spread
        Material.ZOMBIE_HEAD,       // mob-spawning
        Material.SHIELD,            // mob-damage
        Material.LAVA_BUCKET,       // lava-flow
        Material.WATER_BUCKET,      // water-flow
        Material.PISTON,            // piston
        Material.OAK_LEAVES,        // leaf-decay
        Material.LEVER              // use
    };

    public static void openMain(Player player, Land land) {
        LangManager lang = BellLands.getInstance().getLangManager();

        String title = lang.getRaw("gui-title",
            "x", land.getChunkX(), "z", land.getChunkZ());
        Inventory inv = Bukkit.createInventory(null, 27,
            lang.colorize(title));

        // Slot 4: info
        String ownerName = Bukkit.getOfflinePlayer(land.getOwner()).getName();
        if (ownerName == null) ownerName = lang.getRaw("info-unknown-owner");

        ItemStack info = item(Material.OAK_SIGN,
            lang.getRaw("gui-info-name"),
            lang.getRaw("gui-info-owner", "owner", ownerName),
            lang.getRaw("gui-info-chunk", "x", land.getChunkX(), "z", land.getChunkZ()),
            lang.getRaw("gui-info-world", "world", land.getWorldName()),
            lang.getRaw("gui-info-trusted-count", "count", land.getTrusted().size())
        );
        inv.setItem(4, info);

        // Slots 9-18: flags
        String[] allFlags = Land.ALL_FLAGS;
        for (int i = 0; i < allFlags.length; i++) {
            String flag = allFlags[i];
            boolean value = land.getFlag(flag);
            String flagName = lang.getRaw("gui-flag-" + flag);
            String status = value ? lang.getRaw("gui-flag-on") : lang.getRaw("gui-flag-off");
            String lore = lang.getRaw("gui-flag-lore");

            ItemStack flagItem = item(FLAG_MATERIALS[i], flagName, status, lore);
            inv.setItem(9 + i, flagItem);
        }

        // Slot 20: add trusted (green wool)
        ItemStack addTrusted = item(Material.LIME_WOOL,
            lang.getRaw("gui-add-trusted-name"),
            lang.getRaw("gui-add-trusted-lore"));
        inv.setItem(20, addTrusted);

        // Slot 22: trusted players list / manage
        List<String> trustedLore = new ArrayList<>();
        if (land.getTrusted().isEmpty()) {
            trustedLore.add(lang.getRaw("gui-trusted-lore-empty"));
        } else {
            for (UUID uuid : land.getTrusted()) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                if (name == null) name = uuid.toString().substring(0, 8);
                trustedLore.add(lang.getRaw("gui-trusted-lore-player", "player", name));
            }
        }
        if (!land.getTrusted().isEmpty()) {
            trustedLore.add("");
            trustedLore.add(lang.getRaw("gui-trusted-lore-hint"));
        }

        ItemStack trusted = item(Material.PLAYER_HEAD,
            lang.getRaw("gui-trusted-name"),
            trustedLore.toArray(String[]::new));
        inv.setItem(22, trusted);

        player.openInventory(inv);
        ClaimGuiListener.markOpen(player.getUniqueId(), ClaimGuiListener.GuiType.MAIN);
    }

    public static void openAddTrusted(Player player, Land land) {
        LangManager lang = BellLands.getInstance().getLangManager();

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.remove(player);
        online.removeIf(p -> land.isTrusted(p.getUniqueId()));

        int size = Math.max(9, ((online.size() + 1) / 9 + 1) * 9);
        if (size > 54) size = 54;

        Inventory inv = Bukkit.createInventory(null, size,
            lang.colorize(lang.getRaw("gui-add-trusted-title")));

        int slot = 0;
        for (Player p : online) {
            if (slot >= size - 1) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(p);
            meta.displayName(lang.colorize("&a" + p.getName()));
            meta.lore(List.of(lang.colorize(lang.getRaw("gui-add-trusted-lore"))));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        inv.setItem(size - 1, item(Material.ARROW, lang.getRaw("gui-back")));

        player.openInventory(inv);
        ClaimGuiListener.markOpen(player.getUniqueId(), ClaimGuiListener.GuiType.ADD_TRUSTED);
    }

    public static void openRemoveTrusted(Player player, Land land) {
        LangManager lang = BellLands.getInstance().getLangManager();

        Set<UUID> trustedSet = land.getTrusted();
        int size = Math.max(9, ((trustedSet.size() + 1) / 9 + 1) * 9);
        if (size > 54) size = 54;

        Inventory inv = Bukkit.createInventory(null, size,
            lang.colorize(lang.getRaw("gui-remove-trusted-title")));

        int slot = 0;
        for (UUID uuid : trustedSet) {
            if (slot >= size - 1) break;
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) name = uuid.toString().substring(0, 8);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            meta.displayName(lang.colorize("&e" + name));
            meta.lore(List.of(lang.colorize(lang.getRaw("gui-remove-trusted-lore"))));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        inv.setItem(size - 1, item(Material.ARROW, lang.getRaw("gui-back")));

        player.openInventory(inv);
        ClaimGuiListener.markOpen(player.getUniqueId(), ClaimGuiListener.GuiType.REMOVE_TRUSTED);
    }

    private static ItemStack item(Material material, String name, String... lore) {
        LangManager lang = BellLands.getInstance().getLangManager();
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(lang.colorize(name));
        if (lore.length > 0) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(lang.colorize(line));
            }
            meta.lore(loreComponents);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
