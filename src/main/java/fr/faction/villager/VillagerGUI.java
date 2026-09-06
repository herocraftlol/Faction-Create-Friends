package fr.faction.villager;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GUI /faction villageois : liste des villageois recrutés par la faction,
 * puis fiche détaillée par villageois avec inventaire restreint selon son rôle.
 */
public class VillagerGUI implements Listener {

    private enum SlotKind { RESOURCE, WEAPON, HELMET, CHESTPLATE, LEGGINGS, BOOTS, FOOD }

    private static class ListHolder implements InventoryHolder {
        Inventory inv;
        public Inventory getInventory() { return inv; }
    }

    private static class DetailHolder implements InventoryHolder {
        final UUID villagerId;
        Inventory inv;
        final Map<Integer, SlotKind> slotKinds = new HashMap<>();
        final Map<Integer, Integer> resourceIndex = new HashMap<>();
        DetailHolder(UUID villagerId) { this.villagerId = villagerId; }
        public Inventory getInventory() { return inv; }
    }

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final VillagerManager villagerManager;
    private final NamespacedKey idKey;
    private final Map<UUID, UUID> pendingRename = new HashMap<>();

    public VillagerGUI(JavaPlugin plugin, FactionManager factionManager, VillagerManager villagerManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.villagerManager = villagerManager;
        this.idKey = new NamespacedKey(plugin, "villager_id");
    }

    // ════════════════════════════════════════════════════════════════════════
    // LISTE
    // ════════════════════════════════════════════════════════════════════════

    public void openList(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + "§cTu n'es pas dans une faction."); return; }

        List<RecruitedVillager> list = villagerManager.getFactionVillagers(faction.getName());
        ListHolder holder = new ListHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.translateAlternateColorCodes('&',
                "&8&l[&6Villageois&8&l] &f" + faction.getName()));
        holder.inv = inv;

        int slot = 0;
        for (RecruitedVillager rv : list) {
            if (slot >= 45) break;
            inv.setItem(slot++, buildListIcon(rv));
        }
        if (list.isEmpty()) {
            inv.setItem(22, makeItem(Material.VILLAGER_SPAWN_EGG, "§7Aucun villageois recruté",
                    "§7Vise un villageois (8 blocs max)",
                    "§7et tape §e/fac recruter"));
        }
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        inv.setItem(49, makeItem(Material.BARRIER, "§cFermer"));

        player.openInventory(inv);
    }

    private ItemStack buildListIcon(RecruitedVillager rv) {
        Entity e = Bukkit.getEntity(rv.getEntityId());
        List<String> lore = new ArrayList<>();
        lore.add("§7Rôle : " + roleColor(rv.getRole()) + rv.getRole().displayName());
        if (e instanceof Villager v && !v.isDead()) {
            lore.add("§7Vie : §c" + (int) v.getHealth() + " ❤");
            lore.add("§a● Chunk chargé");
        } else {
            lore.add("§8○ Chunk non chargé");
        }
        lore.add("");
        lore.add("§eClic → gérer");
        ItemStack item = makeItem(Material.VILLAGER_SPAWN_EGG, "§e" + rv.getDisplayName(), lore.toArray(new String[0]));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, rv.getEntityId().toString());
            item.setItemMeta(meta);
        }
        return item;
    }

    // ════════════════════════════════════════════════════════════════════════
    // FICHE DÉTAILLÉE
    // ════════════════════════════════════════════════════════════════════════

    public void openDetail(Player player, RecruitedVillager rv) {
        Faction faction = factionManager.getFaction(rv.getFactionName());
        boolean canManage = faction != null && faction.canManage(player.getUniqueId());

        DetailHolder holder = new DetailHolder(rv.getEntityId());
        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.translateAlternateColorCodes('&',
                "&8&l[&6" + rv.getDisplayName() + "&8&l] &f" + rv.getRole().displayName()));
        holder.inv = inv;

        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        // Boutons de rôle
        inv.setItem(0, makeItem(Material.BARRIER, "§7Aucun rôle",
                rv.getRole() == VillagerRole.AUCUN ? "§a✔ Actuellement sélectionné" : "§eClic → retirer le rôle" + (canManage ? "" : " §8(chef/sous-chef)")));
        inv.setItem(1, makeItem(Material.BRICKS, "§bConstructeur",
                rv.getRole() == VillagerRole.CONSTRUCTEUR ? "§a✔ Actuellement sélectionné" : "§eClic → devenir constructeur" + (canManage ? "" : " §8(chef/sous-chef)")));
        inv.setItem(2, makeItem(Material.IRON_SWORD, "§cGuerrier",
                rv.getRole() == VillagerRole.GUERRIER ? "§a✔ Actuellement sélectionné" : "§eClic → devenir guerrier" + (canManage ? "" : " §8(chef/sous-chef)")));

        // Info
        Entity e = Bukkit.getEntity(rv.getEntityId());
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Faction : §e" + rv.getFactionName());
        infoLore.add("§7Rôle : " + roleColor(rv.getRole()) + rv.getRole().displayName());
        if (e instanceof Villager v && !v.isDead()) infoLore.add("§7Vie : §c" + (int) v.getHealth() + " ❤");
        else infoLore.add("§8Chunk non chargé");
        inv.setItem(4, makeItem(Material.PLAYER_HEAD, "§e" + rv.getDisplayName(), infoLore.toArray(new String[0])));

        // Renommer
        inv.setItem(8, makeItem(Material.NAME_TAG, "§eRenommer",
                "§7Clic → tape le nouveau nom dans le chat" + (canManage ? "" : " §8(chef/sous-chef)")));

        // Zone de rôle
        if (rv.getRole() == VillagerRole.CONSTRUCTEUR) {
            ItemStack[] res = rv.getResources();
            for (int i = 0; i < 9; i++) {
                int slot = 18 + i;
                inv.setItem(slot, res[i]);
                holder.slotKinds.put(slot, SlotKind.RESOURCE);
                holder.resourceIndex.put(slot, i);
            }
            List<String> zoneLore = new ArrayList<>();
            if (rv.hasZone()) {
                zoneLore.add("§aChantier défini.");
                zoneLore.add("§7Il comble tous les vides de la zone");
                zoneLore.add("§7avec les blocs ci-dessus (construction ET réparation).");
            } else {
                zoneLore.add("§cAucun chantier défini.");
                zoneLore.add("§7Clic pour cliquer-droit 2 coins dans le monde.");
            }
            zoneLore.add("");
            zoneLore.add("§eClic → (re)définir le chantier");
            inv.setItem(40, makeItem(Material.MAP, "§dDéfinir le chantier", zoneLore.toArray(new String[0])));
        } else if (rv.getRole() == VillagerRole.GUERRIER) {
            inv.setItem(20, rv.getWeapon());   holder.slotKinds.put(20, SlotKind.WEAPON);
            inv.setItem(21, rv.getHelmet());   holder.slotKinds.put(21, SlotKind.HELMET);
            inv.setItem(22, rv.getChestplate()); holder.slotKinds.put(22, SlotKind.CHESTPLATE);
            inv.setItem(23, rv.getLeggings()); holder.slotKinds.put(23, SlotKind.LEGGINGS);
            inv.setItem(24, rv.getBoots());    holder.slotKinds.put(24, SlotKind.BOOTS);
        } else {
            inv.setItem(22, makeItem(Material.BARRIER, "§7Aucun rôle assigné",
                    "§7Choisis Constructeur ou Guerrier", "§7en haut pour débloquer l'équipement."));
        }

        // Nourriture (commun à tous les rôles)
        inv.setItem(31, rv.getFood());
        holder.slotKinds.put(31, SlotKind.FOOD);

        // Navigation
        inv.setItem(45, makeItem(Material.ARROW, "§7◀ Retour à la liste"));
        if (canManage) inv.setItem(49, makeItem(Material.TNT, "§c§lLibérer ce villageois", "§7Retire son rôle et son équipement.", "§cAction irréversible."));
        inv.setItem(53, makeItem(Material.BARRIER, "§cFermer"));

        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════
    // CLICS
    // ════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();

        if (topHolder instanceof ListHolder) {
            handleListClick(event, player);
        } else if (topHolder instanceof DetailHolder holder) {
            handleDetailClick(event, player, holder);
        }
    }

    private void handleListClick(InventoryClickEvent event, Player player) {
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
        if (clicked.getType() != Material.VILLAGER_SPAWN_EGG) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        String idStr = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (idStr == null) return;
        RecruitedVillager rv = villagerManager.getByEntity(UUID.fromString(idStr));
        if (rv == null) { player.sendMessage(prefix() + "§cCe villageois n'existe plus."); return; }
        openDetail(player, rv);
    }

    private void handleDetailClick(InventoryClickEvent event, Player player, DetailHolder holder) {
        boolean topClick = event.getClickedInventory() == event.getView().getTopInventory();
        if (!topClick) return; // clic dans l'inventaire du joueur : comportement normal

        RecruitedVillager rv = villagerManager.getByEntity(holder.villagerId);
        if (rv == null) { event.setCancelled(true); player.closeInventory(); return; }

        if (event.isShiftClick()) { event.setCancelled(true); return; }

        int slot = event.getSlot();
        SlotKind kind = holder.slotKinds.get(slot);

        if (kind == null) {
            event.setCancelled(true);
            handleFixedSlotClick(player, rv, slot);
            return;
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() != Material.AIR && !isValidForSlot(kind, cursor.getType())) {
            event.setCancelled(true);
            player.sendMessage(prefix() + "§cCet objet n'est pas accepté dans cet emplacement.");
            return;
        }

        // Laisser Bukkit transférer l'item, puis relire l'état 1 tick plus tard.
        Bukkit.getScheduler().runTask(plugin, () -> persistDetailSlots(holder, rv));
    }

    private void handleFixedSlotClick(Player player, RecruitedVillager rv, int slot) {
        Faction faction = factionManager.getFaction(rv.getFactionName());
        boolean canManage = faction != null && faction.canManage(player.getUniqueId());

        switch (slot) {
            case 0 -> { if (canManage) { villagerManager.setRole(rv, VillagerRole.AUCUN); openDetail(player, rv); } else denyManage(player); }
            case 1 -> { if (canManage) { villagerManager.setRole(rv, VillagerRole.CONSTRUCTEUR); openDetail(player, rv); } else denyManage(player); }
            case 2 -> { if (canManage) { villagerManager.setRole(rv, VillagerRole.GUERRIER); openDetail(player, rv); } else denyManage(player); }
            case 8 -> {
                if (canManage) {
                    pendingRename.put(player.getUniqueId(), rv.getEntityId());
                    player.closeInventory();
                    player.sendMessage(prefix() + "§eTape le nouveau nom de ce villageois dans le chat (ou §cannuler§e).");
                } else denyManage(player);
            }
            case 40 -> {
                if (rv.getRole() == VillagerRole.CONSTRUCTEUR) {
                    player.closeInventory();
                    villagerManager.startZoneSelection(player, rv);
                }
            }
            case 45 -> openList(player);
            case 49 -> {
                if (canManage) {
                    villagerManager.release(rv);
                    player.closeInventory();
                    player.sendMessage(prefix() + "§aVillageois libéré.");
                } else denyManage(player);
            }
            case 53 -> player.closeInventory();
            default -> { }
        }
    }

    private void persistDetailSlots(DetailHolder holder, RecruitedVillager rv) {
        Inventory inv = holder.inv;
        if (inv == null) return;
        for (Map.Entry<Integer, SlotKind> entry : holder.slotKinds.entrySet()) {
            int slot = entry.getKey();
            SlotKind kind = entry.getValue();
            ItemStack item = inv.getItem(slot);
            ItemStack clean = (item == null || item.getType() == Material.AIR) ? null : item;
            switch (kind) {
                case RESOURCE -> {
                    Integer idx = holder.resourceIndex.get(slot);
                    if (idx != null) rv.getResources()[idx] = clean;
                }
                case WEAPON -> rv.setWeapon(clean);
                case HELMET -> rv.setHelmet(clean);
                case CHESTPLATE -> rv.setChestplate(clean);
                case LEGGINGS -> rv.setLeggings(clean);
                case BOOTS -> rv.setBoots(clean);
                case FOOD -> rv.setFood(clean);
            }
        }
        villagerManager.syncLiveEntity(rv);
        villagerManager.markDirty();
    }

    private void denyManage(Player player) {
        player.sendMessage(prefix() + "§cRéservé au chef ou à un sous-chef.");
    }

    // ════════════════════════════════════════════════════════════════════════
    // RENOMMAGE PAR CHAT
    // ════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID villagerId = pendingRename.get(player.getUniqueId());
        if (villagerId == null) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();
        pendingRename.remove(player.getUniqueId());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (msg.equalsIgnoreCase("annuler")) { player.sendMessage(prefix() + "§7Renommage annulé."); return; }
            RecruitedVillager rv = villagerManager.getByEntity(villagerId);
            if (rv == null) { player.sendMessage(prefix() + "§cVillageois introuvable."); return; }
            String clean = ChatColor.stripColor(msg);
            if (clean.length() > 24) clean = clean.substring(0, 24);
            villagerManager.rename(rv, clean);
            player.sendMessage(prefix() + "§a✔ Renommé en §e" + clean + "§a.");
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // VALIDATION D'OBJETS PAR EMPLACEMENT
    // ════════════════════════════════════════════════════════════════════════

    private boolean isValidForSlot(SlotKind kind, Material type) {
        String name = type.name();
        return switch (kind) {
            case RESOURCE -> type != Material.AIR && type.isBlock();
            case WEAPON -> name.endsWith("_SWORD") || (name.endsWith("_AXE") && !name.contains("PICK")) || type == Material.TRIDENT;
            case HELMET -> name.endsWith("_HELMET") || type == Material.TURTLE_HELMET;
            case CHESTPLATE -> name.endsWith("_CHESTPLATE") || type == Material.ELYTRA;
            case LEGGINGS -> name.endsWith("_LEGGINGS");
            case BOOTS -> name.endsWith("_BOOTS");
            case FOOD -> isEdible(type);
        };
    }

    private boolean isEdible(Material type) {
        try { return type.isEdible(); } catch (Throwable t) { return false; }
    }

    private String roleColor(VillagerRole role) {
        return switch (role) {
            case CONSTRUCTEUR -> "§b";
            case GUERRIER -> "§c";
            default -> "§7";
        };
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILS
    // ════════════════════════════════════════════════════════════════════════

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String l : lore) loreList.add(ChatColor.translateAlternateColorCodes('&', l));
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.prefix", "&8[&6Faction&8] &r"));
    }
}
