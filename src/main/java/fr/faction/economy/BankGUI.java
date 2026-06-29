package fr.faction.economy;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI banque émeraudes.
 *
 * Menu principal → choix Compte Personnel / Coffre Faction
 *   Compte Personnel : dépôt / retrait (1, 16, 64, tout)
 *   Coffre Faction   : dépôt / retrait (chef ou membre selon config)
 */
public class BankGUI implements Listener {

    private static final String TITLE_BANK    = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Banque — Émeraudes";
    private static final String TITLE_PERSO   = ChatColor.GREEN + "" + ChatColor.BOLD    + "Banque Personnelle";
    private static final String TITLE_FACTION = ChatColor.GOLD  + "" + ChatColor.BOLD    + "Coffre Faction";

    private final JavaPlugin plugin;
    private final EmeraldBankManager bankManager;
    private final FactionManager factionManager;

    /** Quel menu le joueur a ouvert */
    private final Map<UUID, String> openMenu = new HashMap<>();

    public BankGUI(JavaPlugin plugin, EmeraldBankManager bankManager, FactionManager factionManager) {
        this.plugin = plugin;
        this.bankManager = bankManager;
        this.factionManager = factionManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ── Ouvertures ────────────────────────────────────────────────────────────

    public void openMainBankMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_BANK);
        openMenu.put(player.getUniqueId(), "main");

        fillBorder(inv, 27, Material.CYAN_STAINED_GLASS_PANE);

        long perso = bankManager.getPlayerBalance(player.getUniqueId());

        inv.setItem(11, makeItem(Material.EMERALD,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Compte Personnel",
                ChatColor.GRAY + "Solde : §a" + perso + " 💎 émeraude(s)",
                "",
                ChatColor.YELLOW + "Clic pour gérer ton compte."));

        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction != null) {
            long fac = bankManager.getFactionBalance(faction.getName());
            inv.setItem(15, makeItem(Material.EMERALD_BLOCK,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "Coffre Faction — " + faction.getName(),
                    ChatColor.GRAY + "Solde : §6" + fac + " 💎 émeraude(s)",
                    "",
                    ChatColor.YELLOW + "Clic pour gérer le coffre faction."));
        } else {
            inv.setItem(15, makeItem(Material.BARRIER,
                    ChatColor.RED + "Aucune faction",
                    ChatColor.GRAY + "Rejoins une faction pour accéder",
                    ChatColor.GRAY + "au coffre partagé."));
        }

        player.openInventory(inv);
    }

    private void openPersonalMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PERSO);
        openMenu.put(player.getUniqueId(), "perso");

        fillBorder(inv, 54, Material.GREEN_STAINED_GLASS_PANE);

        long bal = bankManager.getPlayerBalance(player.getUniqueId());
        int inHand = countEmeralds(player);

        // Info
        inv.setItem(4, makeItem(Material.EMERALD,
                ChatColor.GREEN + "Mon Compte",
                ChatColor.GRAY + "Solde banque : §a" + bal + " 💎",
                ChatColor.GRAY + "Émeraudes en inventaire : §a" + inHand));

        // Dépôts
        inv.setItem(19, makeDepositItem(1,  "Déposer 1 émeraude"));
        inv.setItem(20, makeDepositItem(16, "Déposer 16 émeraudes"));
        inv.setItem(21, makeDepositItem(64, "Déposer 64 émeraudes"));
        inv.setItem(22, makeItem(Material.HOPPER,
                ChatColor.AQUA + "Déposer TOUT",
                ChatColor.GRAY + "Dépose toutes tes émeraudes.",
                ChatColor.GRAY + "Inventaire : §a" + inHand));

        // Retraits
        inv.setItem(28, makeWithdrawItem(1,  "Retirer 1 émeraude"));
        inv.setItem(29, makeWithdrawItem(16, "Retirer 16 émeraudes"));
        inv.setItem(30, makeWithdrawItem(64, "Retirer 64 émeraudes"));
        inv.setItem(31, makeItem(Material.DROPPER,
                ChatColor.RED + "Retirer TOUT",
                ChatColor.GRAY + "Retire toutes tes émeraudes.",
                ChatColor.GRAY + "Solde : §a" + bal));

        // Retour
        inv.setItem(49, makeItem(Material.ARROW,
                ChatColor.GRAY + "← Retour"));

        player.openInventory(inv);
    }

    private void openFactionMenu(Player player, Faction faction) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_FACTION);
        openMenu.put(player.getUniqueId(), "faction");

        fillBorder(inv, 54, Material.GOLD_BLOCK);

        long bal = bankManager.getFactionBalance(faction.getName());
        int inHand = countEmeralds(player);
        boolean isChef = faction.isChef(player.getUniqueId());

        inv.setItem(4, makeItem(Material.EMERALD_BLOCK,
                ChatColor.GOLD + "Coffre Faction — " + faction.getName(),
                ChatColor.GRAY + "Solde : §6" + bal + " 💎",
                ChatColor.GRAY + "Tes émeraudes : §a" + inHand,
                "",
                isChef ? ChatColor.YELLOW + "★ Tu es Chef" : ChatColor.GRAY + "Membre"));

        // Dépôts (tous les membres)
        inv.setItem(19, makeDepositItem(1,  "Déposer 1 →  Faction"));
        inv.setItem(20, makeDepositItem(16, "Déposer 16 → Faction"));
        inv.setItem(21, makeDepositItem(64, "Déposer 64 → Faction"));
        inv.setItem(22, makeItem(Material.HOPPER,
                ChatColor.AQUA + "Déposer TOUT → Faction",
                ChatColor.GRAY + "Inventaire : §a" + inHand));

        // Retraits (chef uniquement)
        if (isChef) {
            inv.setItem(28, makeWithdrawItem(1,  "Retirer 1 ← Faction"));
            inv.setItem(29, makeWithdrawItem(16, "Retirer 16 ← Faction"));
            inv.setItem(30, makeWithdrawItem(64, "Retirer 64 ← Faction"));
            inv.setItem(31, makeItem(Material.DROPPER,
                    ChatColor.RED + "Retirer TOUT ← Faction",
                    ChatColor.GRAY + "Solde faction : §6" + bal));
        } else {
            ItemStack locked = makeItem(Material.RED_STAINED_GLASS_PANE,
                    ChatColor.RED + "Retraits réservés au Chef",
                    ChatColor.GRAY + "Seul le chef peut retirer des fonds.");
            for (int s : new int[]{28, 29, 30, 31}) inv.setItem(s, locked);
        }

        inv.setItem(49, makeItem(Material.ARROW, ChatColor.GRAY + "← Retour"));

        player.openInventory(inv);
    }

    // ── Listener de clic ──────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String menu = openMenu.get(player.getUniqueId());
        if (menu == null) return;

        String title = event.getView().getTitle();
        if (!title.equals(TITLE_BANK) && !title.equals(TITLE_PERSO) && !title.equals(TITLE_FACTION)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        switch (menu) {
            case "main"    -> handleMain(player, event.getSlot());
            case "perso"   -> handlePerso(player, event.getSlot());
            case "faction" -> handleFaction(player, event.getSlot());
        }
    }

    private void handleMain(Player player, int slot) {
        if (slot == 11) {
            openPersonalMenu(player);
        } else if (slot == 15) {
            Faction f = factionManager.getPlayerFaction(player.getUniqueId());
            if (f != null) openFactionMenu(player, f);
        }
    }

    private void handlePerso(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        switch (slot) {
            case 19 -> depositPersonal(player, uuid, 1);
            case 20 -> depositPersonal(player, uuid, 16);
            case 21 -> depositPersonal(player, uuid, 64);
            case 22 -> depositPersonal(player, uuid, countEmeralds(player));
            case 28 -> withdrawPersonal(player, uuid, 1);
            case 29 -> withdrawPersonal(player, uuid, 16);
            case 30 -> withdrawPersonal(player, uuid, 64);
            case 31 -> withdrawPersonal(player, uuid, (int) bankManager.getPlayerBalance(uuid));
            case 49 -> Bukkit.getScheduler().runTask(plugin, () -> openMainBankMenu(player));
        }
    }

    private void handleFaction(Player player, int slot) {
        Faction f = factionManager.getPlayerFaction(player.getUniqueId());
        if (f == null) return;
        switch (slot) {
            case 19 -> depositFaction(player, f, 1);
            case 20 -> depositFaction(player, f, 16);
            case 21 -> depositFaction(player, f, 64);
            case 22 -> depositFaction(player, f, countEmeralds(player));
            case 28 -> withdrawFaction(player, f, 1);
            case 29 -> withdrawFaction(player, f, 16);
            case 30 -> withdrawFaction(player, f, 64);
            case 31 -> withdrawFaction(player, f, (int) bankManager.getFactionBalance(f.getName()));
            case 49 -> Bukkit.getScheduler().runTask(plugin, () -> openMainBankMenu(player));
        }
    }

    // ── Logique dépôt/retrait ─────────────────────────────────────────────────

    private void depositPersonal(Player player, UUID uuid, int amount) {
        int removed = removeEmeralds(player, amount);
        if (removed == 0) {
            player.sendMessage(ChatColor.RED + "Tu n'as pas assez d'émeraudes.");
            return;
        }
        bankManager.depositPlayer(uuid, removed);
        player.sendMessage(ChatColor.GREEN + "+" + removed + " 💎 déposés. Solde : §a"
                + bankManager.getPlayerBalance(uuid));
        Bukkit.getScheduler().runTask(plugin, () -> openPersonalMenu(player));
    }

    private void withdrawPersonal(Player player, UUID uuid, long amount) {
        if (amount <= 0) { player.sendMessage(ChatColor.RED + "Solde insuffisant."); return; }
        long toWithdraw = Math.min(amount, bankManager.getPlayerBalance(uuid));
        if (toWithdraw <= 0) { player.sendMessage(ChatColor.RED + "Solde insuffisant."); return; }
        bankManager.withdrawPlayer(uuid, toWithdraw);
        giveEmeralds(player, (int) toWithdraw);
        player.sendMessage(ChatColor.YELLOW + "-" + toWithdraw + " 💎 retirés. Solde : §a"
                + bankManager.getPlayerBalance(uuid));
        Bukkit.getScheduler().runTask(plugin, () -> openPersonalMenu(player));
    }

    private void depositFaction(Player player, Faction faction, int amount) {
        int removed = removeEmeralds(player, amount);
        if (removed == 0) {
            player.sendMessage(ChatColor.RED + "Tu n'as pas assez d'émeraudes.");
            return;
        }
        bankManager.depositFaction(faction.getName(), removed);
        player.sendMessage(ChatColor.GREEN + "+" + removed + " 💎 déposés dans le coffre de §e"
                + faction.getName() + ChatColor.GREEN + ". Solde faction : §6"
                + bankManager.getFactionBalance(faction.getName()));
        Bukkit.getScheduler().runTask(plugin, () -> openFactionMenu(player, faction));
    }

    private void withdrawFaction(Player player, Faction faction, long amount) {
        if (!faction.isChef(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Seul le chef peut retirer des fonds faction.");
            return;
        }
        long toWithdraw = Math.min(amount, bankManager.getFactionBalance(faction.getName()));
        if (toWithdraw <= 0) { player.sendMessage(ChatColor.RED + "Solde faction insuffisant."); return; }
        bankManager.withdrawFaction(faction.getName(), toWithdraw);
        giveEmeralds(player, (int) toWithdraw);
        player.sendMessage(ChatColor.YELLOW + "-" + toWithdraw + " 💎 retirés du coffre de §e"
                + faction.getName() + ChatColor.YELLOW + ". Solde faction : §6"
                + bankManager.getFactionBalance(faction.getName()));
        Bukkit.getScheduler().runTask(plugin, () -> openFactionMenu(player, faction));
    }

    // ── Utils émeraudes inventaire ────────────────────────────────────────────

    private int countEmeralds(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.EMERALD) total += item.getAmount();
        }
        return total;
    }

    private int removeEmeralds(Player player, int amount) {
        int toRemove = Math.min(amount, countEmeralds(player));
        if (toRemove <= 0) return 0;
        int remaining = toRemove;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != Material.EMERALD) continue;
            int take = Math.min(item.getAmount(), remaining);
            item.setAmount(item.getAmount() - take);
            remaining -= take;
        }
        player.updateInventory();
        return toRemove;
    }

    private void giveEmeralds(Player player, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int stack = Math.min(64, remaining);
            player.getInventory().addItem(new ItemStack(Material.EMERALD, stack));
            remaining -= stack;
        }
    }

    // ── Builders d'items ──────────────────────────────────────────────────────

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeDepositItem(int amount, String label) {
        return makeItem(Material.EMERALD,
                ChatColor.AQUA + label,
                ChatColor.GRAY + "Quantité : §a" + amount,
                ChatColor.YELLOW + "Clic pour déposer.");
    }

    private ItemStack makeWithdrawItem(int amount, String label) {
        return makeItem(Material.REDSTONE,
                ChatColor.RED + label,
                ChatColor.GRAY + "Quantité : §c" + amount,
                ChatColor.YELLOW + "Clic pour retirer.");
    }

    private void fillBorder(Inventory inv, int size, Material mat) {
        ItemStack border = makeItem(mat, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = size - 9; i < size; i++) inv.setItem(i, border);
        for (int i = 9; i < size - 9; i += 9)  inv.setItem(i, border);
        for (int i = 17; i < size - 9; i += 9) inv.setItem(i, border);
    }
}
