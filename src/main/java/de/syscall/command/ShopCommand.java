package de.syscall.command;

import de.syscall.SlownFinance;
import de.syscall.data.Shop;
import de.syscall.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ShopCommand implements CommandExecutor, TabCompleter {

    private final SlownFinance plugin;
    private final List<String> subCommands = Arrays.asList("create", "delete", "list", "info", "toggle", "reload", "sell");

    public ShopCommand(SlownFinance plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.component("&cDieser Command kann nur von Spielern ausgeführt werden!"));
            return true;
        }

        if (args.length == 0) {
            showShopHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> {
                if (args.length < 4) {
                    player.sendMessage(ColorUtil.component("&cVerwendung: /shop create <name> <anzahl> <kaufpreis>"));
                    return true;
                }

                String name = args[1];
                try {
                    int amount = Integer.parseInt(args[2]);
                    double buyPrice = Double.parseDouble(args[3]);

                    if (amount <= 0) {
                        player.sendMessage(ColorUtil.component("&cAnzahl muss positiv sein!"));
                        return true;
                    }

                    if (buyPrice <= 0) {
                        player.sendMessage(ColorUtil.component("&cKaufpreis muss positiv sein!"));
                        return true;
                    }

                    plugin.getShopManager().createShop(player, name, amount, buyPrice);
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.component("&cUngültige Zahl für Anzahl oder Preis!"));
                }
                return true;
            }

            case "sell" -> {
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.component("&cVerwendung: /shop sell <name> <verkaufspreis>"));
                    return true;
                }

                String shopName = args[1];
                Shop shop = findShopByName(player, shopName);

                if (shop == null) {
                    player.sendMessage(ColorUtil.component("&cShop nicht gefunden oder du bist nicht der Besitzer!"));
                    return true;
                }

                try {
                    double sellPrice = Double.parseDouble(args[2]);

                    if (sellPrice <= 0) {
                        player.sendMessage(ColorUtil.component("&cVerkaufspreis muss positiv sein!"));
                        return true;
                    }

                    shop.setSellPrice(sellPrice);
                    shop.setSellEnabled(true);
                    plugin.getShopManager().updateShop(shop);

                    player.sendMessage(ColorUtil.component("&aVerkaufspreis auf &6" + String.format("%.2f", sellPrice) + " Coins &agesetzt!"));
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.component("&cUngültige Zahl für den Verkaufspreis!"));
                }
                return true;
            }

            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.component("&cVerwendung: /shop delete <name>"));
                    return true;
                }

                String shopName = args[1];
                Shop shop = findShopByName(player, shopName);

                if (shop == null) {
                    player.sendMessage(ColorUtil.component("&cShop nicht gefunden oder du bist nicht der Besitzer!"));
                    return true;
                }

                if (plugin.getShopManager().deleteShop(shop.getUniqueId())) {
                    player.sendMessage(ColorUtil.component("&aShop erfolgreich gelöscht!"));
                } else {
                    player.sendMessage(ColorUtil.component("&cFehler beim Löschen des Shops!"));
                }
                return true;
            }

            case "list" -> {
                Collection<Shop> playerShops = plugin.getShopManager().getShopsByOwner(player.getUniqueId());

                if (playerShops.isEmpty()) {
                    player.sendMessage(ColorUtil.component("&7Du hast keine Shops."));
                    return true;
                }

                player.sendMessage(ColorUtil.component("&6Deine Shops:"));
                for (Shop shop : playerShops) {
                    String status = shop.isActive() ? "&a✓" : "&c✗";
                    String sellInfo = shop.isSellEnabled() ? " &7(Verkauf: " + String.format("%.2f", shop.getSellPrice()) + ")" : "";
                    player.sendMessage(ColorUtil.component("&7- " + status + " &6" + shop.getName() +
                            " &7(" + shop.getAmount() + "x für " + String.format("%.2f", shop.getBuyPrice()) + " Coins)" + sellInfo));
                }
                return true;
            }

            case "info" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.component("&cVerwendung: /shop info <name>"));
                    return true;
                }

                String shopName = args[1];
                Shop shop = findShopByName(player, shopName);

                if (shop == null) {
                    player.sendMessage(ColorUtil.component("&cShop nicht gefunden oder du bist nicht der Besitzer!"));
                    return true;
                }

                showShopInfo(player, shop);
                return true;
            }

            case "toggle" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.component("&cVerwendung: /shop toggle <name>"));
                    return true;
                }

                String shopName = args[1];
                Shop shop = findShopByName(player, shopName);

                if (shop == null) {
                    player.sendMessage(ColorUtil.component("&cShop nicht gefunden oder du bist nicht der Besitzer!"));
                    return true;
                }

                shop.setActive(!shop.isActive());
                plugin.getShopManager().updateShop(shop);
                String status = shop.isActive() ? "&aaktiviert" : "&cdeaktiviert";
                player.sendMessage(ColorUtil.component("&7Shop " + status + "&7!"));
                return true;
            }

            case "reload" -> {
                if (!player.hasPermission("slownfinance.shop.admin")) {
                    player.sendMessage(ColorUtil.component("&cDu hast keine Berechtigung für diesen Command!"));
                    return true;
                }

                plugin.getShopManager().reloadConfig();
                player.sendMessage(ColorUtil.component("&aShop-Konfiguration neu geladen!"));
                return true;
            }

            default -> {
                showShopHelp(player);
                return true;
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) {
            return completions;
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    if (subCommand.equals("reload") && !player.hasPermission("slownfinance.shop.admin")) {
                        continue;
                    }
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("delete") || subCommand.equals("info") || subCommand.equals("toggle") || subCommand.equals("sell")) {
                String input = args[1].toLowerCase();
                Collection<Shop> playerShops = plugin.getShopManager().getShopsByOwner(player.getUniqueId());

                for (Shop shop : playerShops) {
                    if (shop.getName().toLowerCase().startsWith(input)) {
                        completions.add(shop.getName());
                    }
                }
            } else if (subCommand.equals("create")) {
                completions.add("<name>");
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("create")) {
                completions.addAll(Arrays.asList("1", "16", "32", "64"));
            } else if (subCommand.equals("sell")) {
                completions.addAll(Arrays.asList("5", "10", "25", "50"));
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            completions.addAll(Arrays.asList("10", "50", "100", "500"));
        }

        return completions;
    }

    private Shop findShopByName(Player player, String name) {
        return plugin.getShopManager().getShopsByOwner(player.getUniqueId()).stream()
                .filter(shop -> shop.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private void showShopInfo(Player player, Shop shop) {
        player.sendMessage(ColorUtil.component("&6&l◆ Shop Information ◆"));
        player.sendMessage(ColorUtil.component("&7&m──────────────────────"));
        player.sendMessage(ColorUtil.component("&7Name: &6" + shop.getName()));
        player.sendMessage(ColorUtil.component("&7Item: &6" + shop.getMaterial().name()));
        player.sendMessage(ColorUtil.component("&7Anzahl: &6" + shop.getAmount()));
        player.sendMessage(ColorUtil.component("&7Kaufpreis: &a" + String.format("%.2f", shop.getBuyPrice()) + " Coins"));

        if (shop.isSellEnabled()) {
            player.sendMessage(ColorUtil.component("&7Verkaufspreis: &c" + String.format("%.2f", shop.getSellPrice()) + " Coins"));
        } else {
            player.sendMessage(ColorUtil.component("&7Verkauf: &cDeaktiviert"));
        }

        player.sendMessage(ColorUtil.component("&7Status: " + (shop.isActive() ? "&aAktiv" : "&cInaktiv")));
        player.sendMessage(ColorUtil.component("&7&m──────────────────────"));
    }

    private void showShopHelp(Player player) {
        player.sendMessage(ColorUtil.component("&6&l◆ Shop Commands ◆"));
        player.sendMessage(ColorUtil.component("&7&m───────────────────────"));
        player.sendMessage(ColorUtil.component("&6/shop create <name> <anzahl> <kaufpreis> &7- Shop erstellen"));
        player.sendMessage(ColorUtil.component("&6/shop sell <name> <verkaufspreis> &7- Verkaufspreis setzen"));
        player.sendMessage(ColorUtil.component("&6/shop delete <name> &7- Shop löschen"));
        player.sendMessage(ColorUtil.component("&6/shop list &7- Alle deine Shops"));
        player.sendMessage(ColorUtil.component("&6/shop info <name> &7- Shop-Informationen"));
        player.sendMessage(ColorUtil.component("&6/shop toggle <name> &7- Shop aktivieren/deaktivieren"));
        if (player.hasPermission("slownfinance.shop.admin")) {
            player.sendMessage(ColorUtil.component("&6/shop reload &7- Konfiguration neu laden"));
        }
        player.sendMessage(ColorUtil.component("&7&m───────────────────────"));
        player.sendMessage(ColorUtil.component("&7💡 Schaue auf eine Kiste beim Erstellen!"));
        player.sendMessage(ColorUtil.component("&7💡 Links klick = Kaufen, Rechts klick = Verkaufen"));
    }
}