package fr.faction.gui;

import fr.faction.managers.FactionManager;
import fr.faction.managers.FactionTeleportManager;
import fr.faction.managers.SharedInventoryManager;
import fr.faction.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class FactionGUI implements Listener {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final SharedInventoryManager sharedInvManager;
    private final FactionTeleportManager teleportManager;

    private static final String TITLE_MAIN    = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Menu Faction";
    private static final String TITLE_MEMBERS = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Membres Faction";
    private static final String TITLE_TP      = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Teleportation Faction";
    private static final String TITLE_CONFIRM = ChatColor.DARK_RED  + "" + ChatColor.BOLD + "Confirmation";

    private final Map<UUID, String> openGUI = new HashMap<>();
    private final Map<UUID, String> pendingAction = new HashMap<>();

    public FactionGUI(JavaPlugin plugin, FactionManager factionManager,
                      SharedInventoryManager sharedInvManager,
                      FactionTeleportManager teleportManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.sharedInvManager = sharedInvManager;
        this.teleportManager = teleportManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMainMenu(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MAIN);

        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++)    inv.setItem(i, border);
        for (int i = 45; i < 54; i++)  inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9)   inv.setItem(i, border);
        for (int i = 17; i < 54; i += 9)  inv.setItem(i, border);

        if (faction == null) {
            inv.setItem(22, makeItem(Material.WRITABLE_BOOK,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "Creer une faction",
                    ChatColor.GRAY + "Clique pour creer ta propre faction.",
                    ChatColor.YELLOW + "Usage : /faction create <nom>"));
            inv.setItem(20, makeItem(Material.PAPER,
                    ChatColor.AQUA + "" + ChatColor.BOLD + "Rejoindre une faction",
                    ChatColor.GRAY + "Tu dois avoir ete invite.",
                    ChatColor.YELLOW + "Usage : /faction join <nom>"));
            inv.setItem(24, makeItem(Material.BOOK,
                    ChatColor.YELLOW + "" + ChatColor.BOLD + "Liste des factions",
                    ChatColor.GRAY + "Voir toutes les factions existantes."));
        } else {
            boolean isChef = faction.isChef(player.getUniqueId());
            long online = faction.getMembers().stream()
                    .map(Bukkit::getPlayer)
                    .filter(p -> p != null && p.isOnline()).count();

            List<String> infoLore = new ArrayList<>();
            infoLore.add(ChatColor.GRAY + "Chef : " + ChatColor.WHITE + getPlayerName(faction.getChef()));
            infoLore.add(ChatColor.GRAY + "Membres : " + ChatColor.WHITE + faction.getMemberCount());
            infoLore.add(ChatColor.GRAY + "En ligne : " + ChatColor.GREEN + online);
            infoLore.add("");
            infoLore.add(isChef ? ChatColor.GOLD + "* Tu es le Chef" : ChatColor.GRAY + "Role : Membre");

            inv.setItem(4, makeItemGlowing(Material.GOLDEN_HELMET,
                    ChatColor.GOLD + "" + ChatColor.BOLD + faction.getName(), infoLore));

            inv.setItem(19, makeItem(Material.PLAYER_HEAD,
                    ChatColor.AQUA + "" + ChatColor.BOLD + "Membres",
                    ChatColor.GRAY + "Voir et gerer les membres de ta faction."));

            inv.setItem(21, makeItem(Material.CHEST,
                    ChatColor.YELLOW + "" + ChatColor.BOLD + "Coffre Partage",
                    ChatColor.GRAY + "Acces a l inventaire partage."));

            inv.setItem(23, makeItem(Material.ENDER_PEARL,
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Teleportation",
                    ChatColor.GRAY + "Se teleporter vers un membre."));

            if (isChef) {
                inv.setItem(25, makeItem(Material.TNT,
                        ChatColor.RED + "" + ChatColor.BOLD + "Dissoudre la faction",
                        ChatColor.GRAY + "Supprime definitivement la faction.",
                        ChatColor.RED + "Action irreversible !"));
                inv.setItem(29, makeItem(Material.NAME_TAG,
                        ChatColor.GREEN + "" + ChatColor.BOLD + "Inviter un joueur",
                        ChatColor.GRAY + "Usage : /faction invite <joueur>"));
            } else {
                inv.setItem(25, makeItem(Material.RED_BED,
                        ChatColor.RED + "" + ChatColor.BOLD + "Quitter la faction",
                        ChatColor.GRAY + "Quitte la faction actuelle."));
            }

            inv.setItem(31, makeItem(Material.BOOK,
                    ChatColor.WHITE + "" + ChatColor.BOLD + "Informations",
                    ChatColor.GRAY + "Affiche les infos dans le chat."));

            inv.setItem(33, makeItem(Material.COMPASS,
                    ChatColor.WHITE + "" + ChatColor.BOLD + "Liste des factions",
                    ChatColor.GRAY + "Voir toutes les factions."));
        }

        inv.setItem(49, makeItem(Material.BARRIER, ChatColor.RED + "Fermer"));
        openGUI.put(player.getUniqueId(), "main");
        player.openInventory(inv);
    }

    private void openMembersMenu(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return;

        int size = Math.min(54, Math.max(27, ((faction.getMemberCount() / 9) + 2) * 9));
        Inventory inv = Bukkit.createInventory(null, size, TITLE_MEMBERS);
        boolean isChef = faction.isChef(player.getUniqueId());

        int slot = 0;
        for (UUID uuid : faction.getMembers()) {
            if (slot >= size - 9) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            Player online = Bukkit.getPlayer(uuid);
            boolean isOnline = online != null && online.isOnline();
            boolean isSelf   = uuid.equals(player.getUniqueId());
            boolean memberIsChef = faction.isChef(uuid);

            List<String> lore = new ArrayList<>();
            lore.add(isOnline ? ChatColor.GREEN + "En ligne" : ChatColor.DARK_GRAY + "Hors ligne");
            if (memberIsChef) lore.add(ChatColor.GOLD + "Chef");
            lore.add("");
            if (isOnline && !isSelf) {
                lore.add(ChatColor.YELLOW + "Clic gauche : Teleporter vers ce joueur");
                if (isChef) {
                    lore.add(ChatColor.RED + "Clic droit : Expulser");
                    if (!memberIsChef) lore.add(ChatColor.GOLD + "Shift+clic : Nommer Chef");
                }
            }

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(op);
                String dn = (op.getName() != null ? op.getName() : "Inconnu")
                        + (memberIsChef ? " [Chef]" : "")
                        + (isSelf ? " (Toi)" : "");
                meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + dn);
                meta.setLore(lore);
                skull.setItemMeta(meta);
            }
            inv.setItem(slot++, skull);
        }

        for (int i = size - 9; i < size; i++) inv.setItem(i, makeItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        inv.setItem(size - 5, makeItem(Material.ARROW, ChatColor.GRAY + "Retour au menu principal"));

        openGUI.put(player.getUniqueId(), "members");
        player.openInventory(inv);
    }

    private void openTeleportMenu(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return;

        List<Player> onlineMembers = teleportManager.getOnlineMembersExcept(player);
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_TP);

        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++)   inv.setItem(i, border);
        for (int i = 18; i < 27; i++) inv.setItem(i, border);

        if (onlineMembers.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER,
                    ChatColor.RED + "Aucun membre en ligne",
                    ChatColor.GRAY + "Personne a qui se teleporter."));
        } else {
            inv.setItem(11, makeItemGlowing(Material.ENDER_EYE,
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Le plus proche",
                    ChatColor.GRAY + "Se TP vers le membre le plus proche."));

            int slot = 13;
            for (Player m : onlineMembers) {
                if (slot >= 18) break;
                boolean memberIsChef = faction.isChef(m.getUniqueId());
                double dist = player.getWorld().equals(m.getWorld())
                        ? Math.round(player.getLocation().distance(m.getLocation())) : -1;

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GREEN + "En ligne");
                if (memberIsChef) lore.add(ChatColor.GOLD + "Chef");
                lore.add(dist >= 0
                        ? ChatColor.GRAY + "Distance : " + ChatColor.WHITE + (int) dist + "m"
                        : ChatColor.GRAY + "Monde different");
                lore.add("");
                lore.add(ChatColor.YELLOW + "Clic pour se teleporter");

                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(m);
                    meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + m.getName()
                            + (memberIsChef ? " [Chef]" : ""));
                    meta.setLore(lore);
                    skull.setItemMeta(meta);
                }
                inv.setItem(slot++, skull);
            }
        }

        inv.setItem(22, makeItem(Material.ARROW, ChatColor.GRAY + "Retour au menu principal"));
        openGUI.put(player.getUniqueId(), "tp");
        player.openInventory(inv);
    }

    private void openConfirmMenu(Player player, String action, String description) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CONFIRM);
        ItemStack border = makeItem(Material.RED_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        inv.setItem(11, makeItemGlowing(Material.LIME_WOOL,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Confirmer",
                ChatColor.GRAY + description));
        inv.setItem(13, makeItem(Material.PAPER,
                ChatColor.WHITE + "" + ChatColor.BOLD + description,
                ChatColor.RED + "Action irreversible !"));
        inv.setItem(15, makeItem(Material.RED_WOOL,
                ChatColor.RED + "" + ChatColor.BOLD + "Annuler",
                ChatColor.GRAY + "Revenir au menu principal."));

        pendingAction.put(player.getUniqueId(), action);
        openGUI.put(player.getUniqueId(), "confirm");
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        if (!openGUI.containsKey(uuid)) return;

        String title = event.getView().getTitle();
        boolean isOurGUI = title.equals(TITLE_MAIN) || title.equals(TITLE_MEMBERS)
                || title.equals(TITLE_TP) || title.equals(TITLE_CONFIRM);
        if (!isOurGUI) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Material mat = clicked.getType();
        if (mat == Material.BLACK_STAINED_GLASS_PANE || mat == Material.GRAY_STAINED_GLASS_PANE
                || mat == Material.RED_STAINED_GLASS_PANE) return;

        String itemName = (clicked.hasItemMeta() && clicked.getItemMeta() != null
                && clicked.getItemMeta().hasDisplayName())
                ? clicked.getItemMeta().getDisplayName() : "";
        String stripped = ChatColor.stripColor(itemName);

        // ── MENU PRINCIPAL
        if (title.equals(TITLE_MAIN)) {
            if (stripped.contains("Membres")) {
                player.closeInventory(); openMembersMenu(player);
            } else if (stripped.contains("Coffre Partage")) {
                openGUI.remove(uuid); player.closeInventory();
                sharedInvManager.openSharedInventory(player);
            } else if (stripped.contains("Teleportation")) {
                player.closeInventory(); openTeleportMenu(player);
            } else if (stripped.contains("Dissoudre")) {
                openConfirmMenu(player, "disband", "Dissoudre la faction");
            } else if (stripped.contains("Quitter la faction")) {
                openConfirmMenu(player, "leave", "Quitter la faction");
            } else if (stripped.contains("Informations")) {
                player.closeInventory(); player.performCommand("faction info");
            } else if (stripped.contains("Liste des factions")) {
                player.closeInventory(); player.performCommand("faction list");
            } else if (stripped.contains("Creer une faction")) {
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Utilise " + ChatColor.WHITE
                        + "/faction create <nom>" + ChatColor.YELLOW + " pour creer ta faction.");
            } else if (stripped.contains("Rejoindre une faction")) {
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Utilise " + ChatColor.WHITE
                        + "/faction join <nom>" + ChatColor.YELLOW + " pour rejoindre une faction.");
            } else if (stripped.equals("Fermer")) {
                openGUI.remove(uuid); player.closeInventory();
            }
        }

        // ── MENU MEMBRES
        else if (title.equals(TITLE_MEMBERS)) {
            if (stripped.contains("Retour")) {
                player.closeInventory(); openMainMenu(player); return;
            }
            if (mat == Material.PLAYER_HEAD) {
                Faction faction = factionManager.getPlayerFaction(uuid);
                if (faction == null) return;
                boolean isChef = faction.isChef(uuid);

                String rawName = stripped.replace(" [Chef]", "").replace(" (Toi)", "").trim();
                Player target = Bukkit.getPlayer(rawName);
                if (target == null || target.equals(player)) return;
                if (!faction.isMember(target.getUniqueId())) return;

                if (event.isShiftClick() && isChef && !faction.isChef(target.getUniqueId())) {
                    player.closeInventory();
                    factionManager.setChef(faction.getName(), target.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + target.getName() + " est maintenant chef !");
                    target.sendMessage(ChatColor.GOLD + "Tu es maintenant chef de " + faction.getName() + " !");
                } else if (event.isRightClick() && isChef) {
                    player.closeInventory();
                    factionManager.removeMember(faction.getName(), target.getUniqueId());
                    player.sendMessage(ChatColor.YELLOW + target.getName() + " expulse de la faction.");
                    target.sendMessage(ChatColor.RED + "Tu as ete expulse de la faction " + faction.getName() + ".");
                } else if (event.isLeftClick()) {
                    player.closeInventory();
                    teleportManager.teleportToMember(player, target.getName());
                }
            }
        }

        // ── MENU TP
        else if (title.equals(TITLE_TP)) {
            if (stripped.contains("Retour")) {
                player.closeInventory(); openMainMenu(player);
            } else if (stripped.contains("Le plus proche")) {
                player.closeInventory(); teleportManager.teleportToNearest(player);
            } else if (mat == Material.PLAYER_HEAD) {
                String rawName = stripped.replace(" [Chef]", "").trim();
                player.closeInventory(); teleportManager.teleportToMember(player, rawName);
            }
        }

        // ── CONFIRMATION
        else if (title.equals(TITLE_CONFIRM)) {
            String action = pendingAction.get(uuid);
            if (stripped.equals("Confirmer") && action != null) {
                player.closeInventory(); pendingAction.remove(uuid);
                if ("disband".equals(action)) {
                    Faction faction = factionManager.getPlayerFaction(uuid);
                    if (faction != null && faction.isChef(uuid)) {
                        String name = faction.getName();
                        for (UUID m : new ArrayList<>(faction.getMembers())) {
                            Player mp = Bukkit.getPlayer(m);
                            if (mp != null && !mp.equals(player))
                                mp.sendMessage(ChatColor.RED + "La faction " + name + " a ete dissoute.");
                        }
                        factionManager.disbandFaction(name);
                        player.sendMessage(ChatColor.YELLOW + "Faction " + name + " dissoute.");
                    }
                } else if ("leave".equals(action)) {
                    Faction faction = factionManager.getPlayerFaction(uuid);
                    if (faction != null) {
                        String name = faction.getName();
                        if (faction.isChef(uuid) && faction.getMemberCount() > 1) {
                            player.sendMessage(ChatColor.RED + "Transfere d abord le role de chef.");
                        } else {
                            factionManager.removeMember(name, uuid);
                            player.sendMessage(ChatColor.YELLOW + "Tu as quitte la faction " + name + ".");
                        }
                    }
                }
            } else if (stripped.equals("Annuler")) {
                pendingAction.remove(uuid); player.closeInventory(); openMainMenu(player);
            }
        }
    }

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        return makeItem(mat, name, lore.toArray(new String[0]));
    }

    @SuppressWarnings("deprecation")
    private ItemStack makeItemGlowing(Material mat, String name, String... lore) {
        ItemStack item = makeItem(mat, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeItemGlowing(Material mat, String name, List<String> lore) {
        return makeItemGlowing(mat, name, lore.toArray(new String[0]));
    }

    private String getPlayerName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : "Inconnu";
    }
}
