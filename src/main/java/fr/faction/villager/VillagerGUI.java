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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * GUI /faction villageois : liste des villageois recrutés par la faction,
 * puis fiche détaillée par villageois avec inventaire restreint selon son rôle.
 */
public class VillagerGUI implements Listener {

    private enum SlotKind { RESOURCE, TASK_TYPE, WEAPON, HELMET, CHESTPLATE, LEGGINGS, BOOTS, BOW, ARROWS, LOOT, TOOL, FOOD }

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

    private enum PendingType { RENAME, RADIUS }
    private static class PendingChatInput {
        final UUID villagerId;
        final PendingType type;
        PendingChatInput(UUID villagerId, PendingType type) { this.villagerId = villagerId; this.type = type; }
    }

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final VillagerManager villagerManager;
    private final NamespacedKey idKey;
    private final Map<UUID, PendingChatInput> pendingChat = new HashMap<>();
    private final Map<UUID, Boolean> selectionMode = new HashMap<>();
    private final Map<UUID, Set<UUID>> selectedVillagers = new HashMap<>();

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

        boolean selecting = selectionMode.getOrDefault(player.getUniqueId(), false);
        Set<UUID> selected = selectedVillagers.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());

        List<RecruitedVillager> list = villagerManager.getFactionVillagers(faction.getName());
        ListHolder holder = new ListHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.translateAlternateColorCodes('&',
                "&8&l[&6Villageois&8&l] &f" + faction.getName() + (selecting ? " &7(sélection)" : "")));
        holder.inv = inv;

        int slot = 0;
        for (RecruitedVillager rv : list) {
            if (slot >= 45) break;
            inv.setItem(slot++, buildListIcon(rv, selecting && selected.contains(rv.getEntityId())));
        }
        if (list.isEmpty()) {
            inv.setItem(22, makeItem(Material.VILLAGER_SPAWN_EGG, "§7Aucun villageois recruté",
                    "§7Vise un villageois (8 blocs max)",
                    "§7et tape §e/fac recruter"));
        }
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        inv.setItem(45, makeItem(selecting ? Material.LIME_DYE : Material.GRAY_DYE,
                selecting ? "§aMode sélection : activé" : "§7Mode sélection : désactivé",
                "§7Sélectionne plusieurs villageois pour",
                "§7leur assigner un poste ou un chantier commun.",
                "§eClic → " + (selecting ? "désactiver" : "activer")));
        inv.setItem(46, makeItem(Material.COMPASS, "§dPoste commun (guerriers sélectionnés)",
                "§7Assigne le même poste à tous les guerriers", "§7actuellement sélectionnés."));
        inv.setItem(47, makeItem(Material.WRITABLE_BOOK, "§dChantier commun (constructeurs sélectionnés)",
                "§7Tiens le bloc voulu en main, puis clique ici.",
                "§7Assigne le même chantier à tous les constructeurs", "§7actuellement sélectionnés."));
        inv.setItem(48, makeItem(Material.OAK_SIGN, "§eRanger en ligne",
                "§7Aligne devant toi tous tes villageois", "§7à moins de 40 blocs."));
        inv.setItem(49, makeItem(Material.BARRIER, "§cFermer"));
        inv.setItem(50, makeItem(Material.LAVA_BUCKET, "§cVider la sélection",
                "§7" + selected.size() + " villageois actuellement sélectionné(s)."));
        inv.setItem(51, makeItem(Material.CAMPFIRE, "§dRassemblement commun (sélection)",
                "§7Assigne le même point de rassemblement à tous", "§7les villageois actuellement sélectionnés."));
        inv.setItem(52, makeItem(Material.SHIELD, "§eRanger en cercle",
                "§7Dispose autour de toi tous tes villageois", "§7à moins de 40 blocs."));

        player.openInventory(inv);
    }

    private ItemStack buildListIcon(RecruitedVillager rv, boolean selected) {
        Entity e = Bukkit.getEntity(rv.getEntityId());
        List<String> lore = new ArrayList<>();
        lore.add("§7Rôle : " + roleColor(rv.getRole()) + rv.getRole().displayName() + " §7(Nv." + rv.getLevel() + ")");
        if (rv.getRole() == VillagerRole.GUERRIER) lore.add("§7Ennemis tués : §c" + rv.getKillCount());
        if (e instanceof Villager v && !v.isDead()) {
            lore.add("§7Vie : §c" + (int) v.getHealth() + " ❤");
            lore.add("§a● Chunk chargé");
        } else {
            lore.add("§8○ Chunk non chargé");
        }
        lore.add("");
        if (selected) lore.add("§a✔ Sélectionné §7(clic → désélectionner)");
        else lore.add("§eClic → gérer §7(mode sélection : clic → sélectionner)");
        ItemStack item = makeItem(Material.VILLAGER_SPAWN_EGG, "§e" + rv.getDisplayName(), lore.toArray(new String[0]));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, rv.getEntityId().toString());
            item.setItemMeta(meta);
        }
        if (selected) addGlow(item);
        return item;
    }

    /** Fausse lueur d'enchantement (sans afficher le nom de l'enchant) pour marquer visuellement une sélection. */
    private void addGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    // ════════════════════════════════════════════════════════════════════════
    // FICHE DÉTAILLÉE
    // ════════════════════════════════════════════════════════════════════════

    // ════════════════════════════════════════════════════════════════════════
    // OUVERTURE PAR CLIC-DROIT DIRECT SUR LE VILLAGEOIS
    // ════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager v)) return;
        RecruitedVillager rv = villagerManager.getByEntity(v.getUniqueId());
        if (rv == null) return; // villageois normal : laisser le commerce vanille se faire
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return; // évite le double-appel main/off-hand

        event.setCancelled(true); // empêche l'ouverture du commerce vanille sur nos recrues

        Player player = event.getPlayer();
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null || !faction.getName().equalsIgnoreCase(rv.getFactionName())) {
            player.sendMessage(prefix() + "§cCe villageois appartient à la faction §e" + rv.getFactionName() + "§c.");
            return;
        }
        openDetail(player, rv);
    }

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
        inv.setItem(3, makeItem(Material.IRON_PICKAXE, "§aRécolteur",
                rv.getRole() == VillagerRole.RECOLTEUR ? "§a✔ Actuellement sélectionné" : "§eClic → devenir récolteur" + (canManage ? "" : " §8(chef/sous-chef)")));

        // Info
        Entity e = Bukkit.getEntity(rv.getEntityId());
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Faction : §e" + rv.getFactionName());
        infoLore.add("§7Rôle : " + roleColor(rv.getRole()) + rv.getRole().displayName());
        infoLore.add("§7Niveau : §e" + rv.getLevel() + "§7/5 §8(" + rv.getXp() + " XP)");
        if (rv.getRole() == VillagerRole.GUERRIER) infoLore.add("§7Ennemis tués : §c" + rv.getKillCount());
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

            String manageTag = canManage ? "" : " §8(chef/sous-chef)";

            // Emplacement pour choisir le type de bloc du PROCHAIN chantier
            inv.setItem(27, null);
            holder.slotKinds.put(27, SlotKind.TASK_TYPE);

            inv.setItem(33, makeItem(Material.WRITABLE_BOOK, "§dAjouter un chantier",
                    "§7Place d'abord un bloc dans l'emplacement",
                    "§7juste au-dessus (type de bloc voulu),",
                    "§7puis clique ici et clique-droit 2 coins dans le monde." + manageTag));

            BuildTask current = rv.getCurrentTask();
            List<String> taskLore = new ArrayList<>();
            if (current != null) {
                taskLore.add("§aChantier actif : §e" + prettyMaterial(current.getBlockType()));
                if (current.getAssignedByName() != null) taskLore.add("§7Assigné par : §f" + current.getAssignedByName());
            } else {
                taskLore.add("§7Aucun chantier en cours.");
            }
            int queued = rv.getTaskQueue().size();
            taskLore.add("§7File d'attente : §e" + queued + " chantier(s)");
            inv.setItem(36, makeItem(Material.FILLED_MAP, "§dChantier actuel", taskLore.toArray(new String[0])));

            inv.setItem(37, makeItem(Material.BARRIER, "§cAnnuler le chantier actuel",
                    "§7Passe directement au suivant dans la file." + manageTag));
            inv.setItem(38, makeItem(Material.LAVA_BUCKET, "§cVider toute la file",
                    "§7Annule le chantier actif ET tous ceux en attente." + manageTag));

            List<String> gatherLore = new ArrayList<>();
            if (rv.hasGatherZone()) {
                gatherLore.add("§aZone de récolte définie.");
                gatherLore.add("§7S'il n'a plus le bloc voulu, il ira le miner ici.");
            } else {
                gatherLore.add("§cAucune zone de récolte définie.");
            }
            gatherLore.add("§eClic → (re)définir cette zone" + manageTag);
            inv.setItem(40, makeItem(Material.IRON_PICKAXE, "§dZone de récolte", gatherLore.toArray(new String[0])));

            if (rv.hasGatherZone()) {
                inv.setItem(41, makeItem(Material.BARRIER, "§cAnnuler la zone de récolte", manageTag.isEmpty() ? "§7Clic pour retirer." : "§7Clic pour retirer." + manageTag));
            }

            List<String> rallyLore = new ArrayList<>();
            if (rv.getRallyPoint() != null) rallyLore.add("§aDéfini. §7Il y retourne une fois sa file de chantiers vide.");
            else rallyLore.add("§7Non défini : il reste sur place une fois sa file vide.");
            rallyLore.add("§eClic → clique-droit dans le monde" + manageTag);
            inv.setItem(43, makeItem(Material.OAK_SIGN, "§dPoint de rassemblement", rallyLore.toArray(new String[0])));
            if (rv.getRallyPoint() != null) {
                inv.setItem(44, makeItem(Material.BARRIER, "§cAnnuler le rassemblement", "§7Clic pour retirer." + manageTag));
            }
        } else if (rv.getRole() == VillagerRole.GUERRIER) {
            inv.setItem(18, rv.getBow());       holder.slotKinds.put(18, SlotKind.BOW);
            inv.setItem(19, rv.getArrows());    holder.slotKinds.put(19, SlotKind.ARROWS);
            inv.setItem(20, rv.getWeapon());   holder.slotKinds.put(20, SlotKind.WEAPON);
            inv.setItem(21, rv.getHelmet());   holder.slotKinds.put(21, SlotKind.HELMET);
            inv.setItem(22, rv.getChestplate()); holder.slotKinds.put(22, SlotKind.CHESTPLATE);
            inv.setItem(23, rv.getLeggings()); holder.slotKinds.put(23, SlotKind.LEGGINGS);
            inv.setItem(24, rv.getBoots());    holder.slotKinds.put(24, SlotKind.BOOTS);

            // Butin ramassé sur ses victimes (8 des 9 emplacements de sa réserve ; le 9e reste "en poche")
            ItemStack[] loot = rv.getResources();
            int[] lootSlots = {27, 28, 29, 30, 32, 33, 34, 35};
            for (int i = 0; i < lootSlots.length; i++) {
                inv.setItem(lootSlots[i], loot[i]);
                holder.slotKinds.put(lootSlots[i], SlotKind.LOOT);
                holder.resourceIndex.put(lootSlots[i], i);
            }

            String manageTag = canManage ? "" : " §8(chef/sous-chef)";
            inv.setItem(36, makeItem(Material.COMPASS, "§dDéfinir le poste ici",
                    "§7Centre du périmètre de défense.", "§eClic → utiliser sa position actuelle" + manageTag));
            inv.setItem(37, makeItem(Material.SPYGLASS, "§dRayon de défense",
                    "§7Actuel : §e" + (int) rv.getDefenseRadius() + " blocs",
                    "§eClic → tape un nombre (4-48) dans le chat" + manageTag));
            inv.setItem(38, makeItem(Material.FILLED_MAP, "§dDéfinir une patrouille",
                    rv.getPatrolPoints().isEmpty() ? "§cAucune patrouille définie." : "§a" + rv.getPatrolPoints().size() + " point(s) définis.",
                    "§7Clic-droit dans le monde pour ajouter des points,",
                    "§7shift+clic-droit pour terminer." + manageTag,
                    "§eClic → démarrer la sélection"));
            inv.setItem(39, makeItem(rv.isCombatEnabled() ? Material.SHIELD : Material.BARRIER,
                    rv.isCombatEnabled() ? "§aCombat : activé" : "§cCombat : arrêté",
                    "§7Clic → " + (rv.isCombatEnabled() ? "cesser le combat" : "reprendre le combat") + manageTag));
            inv.setItem(41, makeItem(player.getUniqueId().equals(rv.getFollowTarget()) ? Material.LEAD : Material.STICK,
                    player.getUniqueId().equals(rv.getFollowTarget()) ? "§aTe suit actuellement" : "§7Suivre",
                    "§7Clic → " + (player.getUniqueId().equals(rv.getFollowTarget()) ? "arrêter de te suivre" : "le faire te suivre et te défendre")));
            inv.setItem(42, makeItem(rv.isArcheryMode() ? Material.BOW : Material.ARROW,
                    rv.isArcheryMode() ? "§aMode archerie : activé" : "§7Mode archerie : désactivé",
                    "§7Avec arc + flèches : tire à distance en gardant ses distances.",
                    "§7Repasse seul en mêlée s'il n'a plus de flèches",
                    "§7ou si l'ennemi est au corps à corps.",
                    "§eClic → " + (rv.isArcheryMode() ? "désactiver" : "activer") + manageTag));

            List<String> rallyLore = new ArrayList<>();
            if (rv.getRallyPoint() != null) rallyLore.add("§aDéfini. §7Il y retourne dès qu'il n'a ni cible, ni ronde, ni joueur à suivre.");
            else rallyLore.add("§7Non défini : il retourne à son poste une fois libre.");
            rallyLore.add("§eClic → clique-droit dans le monde" + manageTag);
            inv.setItem(43, makeItem(Material.OAK_SIGN, "§dPoint de rassemblement", rallyLore.toArray(new String[0])));
            if (rv.getRallyPoint() != null) {
                inv.setItem(44, makeItem(Material.BARRIER, "§cAnnuler le rassemblement", "§7Clic pour retirer." + manageTag));
            }
        } else if (rv.getRole() == VillagerRole.RECOLTEUR) {
            inv.setItem(18, rv.getTool());
            holder.slotKinds.put(18, SlotKind.TOOL);

            ItemStack[] stock = rv.getResources();
            int[] stockSlots = {19, 20, 21, 22, 23, 24, 25, 26};
            for (int i = 0; i < stockSlots.length; i++) {
                inv.setItem(stockSlots[i], stock[i]);
                holder.slotKinds.put(stockSlots[i], SlotKind.LOOT);
                holder.resourceIndex.put(stockSlots[i], i);
            }

            String manageTag = canManage ? "" : " §8(chef/sous-chef)";

            List<String> harvestLore = new ArrayList<>();
            if (rv.hasHarvestZone()) harvestLore.add("§aZone définie. §7Pioche → minerais, hache → bois, pelle → terre/sable/…");
            else harvestLore.add("§cAucune zone définie.");
            harvestLore.add("§eClic → clique-droit 2 coins dans le monde" + manageTag);
            inv.setItem(33, makeItem(Material.IRON_PICKAXE, "§dZone de récolte", harvestLore.toArray(new String[0])));
            if (rv.hasHarvestZone()) {
                inv.setItem(34, makeItem(Material.BARRIER, "§cAnnuler la zone de récolte", "§7Clic pour retirer." + manageTag));
            }

            List<String> farmLore = new ArrayList<>();
            if (rv.hasFarmZone()) farmLore.add("§aChamp défini. §7Donne-lui des graines : il plante, récolte, replante.");
            else farmLore.add("§cAucun champ défini.");
            farmLore.add("§eClic → clique-droit 2 coins dans le monde" + manageTag);
            inv.setItem(36, makeItem(Material.WHEAT, "§dChamp", farmLore.toArray(new String[0])));
            if (rv.hasFarmZone()) {
                inv.setItem(37, makeItem(Material.BARRIER, "§cAnnuler le champ", "§7Clic pour retirer." + manageTag));
            }

            List<String> chestLore = new ArrayList<>();
            if (rv.getOutputChest() != null) chestLore.add("§aCoffre défini : tout ce qu'il récolte y est déposé.");
            else chestLore.add("§cAucun coffre défini : il garde tout sur lui.");
            chestLore.add("§eClic → clique-droit sur un coffre dans le monde" + manageTag);
            inv.setItem(39, makeItem(Material.CHEST, "§dCoffre de dépôt", chestLore.toArray(new String[0])));
            if (rv.getOutputChest() != null) {
                inv.setItem(40, makeItem(Material.BARRIER, "§cAnnuler le coffre", "§7Clic pour retirer." + manageTag));
            }

            List<String> rallyLoreR = new ArrayList<>();
            if (rv.getRallyPoint() != null) rallyLoreR.add("§aDéfini. §7Il y retourne une fois sa réserve gérée.");
            else rallyLoreR.add("§7Non défini : il reste sur place.");
            rallyLoreR.add("§eClic → clique-droit dans le monde" + manageTag);
            inv.setItem(43, makeItem(Material.OAK_SIGN, "§dPoint de rassemblement", rallyLoreR.toArray(new String[0])));
            if (rv.getRallyPoint() != null) {
                inv.setItem(44, makeItem(Material.BARRIER, "§cAnnuler le rassemblement", "§7Clic pour retirer." + manageTag));
            }
        } else {
            inv.setItem(22, makeItem(Material.BARRIER, "§7Aucun rôle assigné",
                    "§7Choisis Constructeur, Guerrier ou Récolteur", "§7en haut pour débloquer l'équipement."));
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

        UUID uuid = player.getUniqueId();
        boolean selecting = selectionMode.getOrDefault(uuid, false);
        Set<UUID> selected = selectedVillagers.computeIfAbsent(uuid, k -> new HashSet<>());

        if (clicked.getType() == Material.VILLAGER_SPAWN_EGG) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;
            String idStr = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
            if (idStr == null) return;
            UUID villagerId = UUID.fromString(idStr);

            if (selecting) {
                if (!selected.remove(villagerId)) selected.add(villagerId);
                openList(player);
                return;
            }
            RecruitedVillager rv = villagerManager.getByEntity(villagerId);
            if (rv == null) { player.sendMessage(prefix() + "§cCe villageois n'existe plus."); return; }
            openDetail(player, rv);
            return;
        }

        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
        if (clicked.getType() == Material.OAK_SIGN) { player.closeInventory(); villagerManager.formation(player, VillagerManager.FormationType.LIGNE); return; }
        if (clicked.getType() == Material.SHIELD) { player.closeInventory(); villagerManager.formation(player, VillagerManager.FormationType.CERCLE); return; }

        if (clicked.getType() == Material.LIME_DYE || clicked.getType() == Material.GRAY_DYE) {
            selectionMode.put(uuid, !selecting);
            openList(player);
            return;
        }

        if (clicked.getType() == Material.LAVA_BUCKET) {
            selected.clear();
            openList(player);
            return;
        }

        if (clicked.getType() == Material.COMPASS) {
            List<UUID> guerrierIds = new ArrayList<>();
            for (UUID id : selected) {
                RecruitedVillager rv = villagerManager.getByEntity(id);
                if (rv != null && rv.getRole() == VillagerRole.GUERRIER) guerrierIds.add(id);
            }
            if (guerrierIds.isEmpty()) { player.sendMessage(prefix() + "§cAucun guerrier dans ta sélection."); return; }
            player.closeInventory();
            villagerManager.startGroupPostSelection(player, guerrierIds);
            return;
        }

        if (clicked.getType() == Material.WRITABLE_BOOK) {
            List<UUID> builderIds = new ArrayList<>();
            for (UUID id : selected) {
                RecruitedVillager rv = villagerManager.getByEntity(id);
                if (rv != null && rv.getRole() == VillagerRole.CONSTRUCTEUR) builderIds.add(id);
            }
            if (builderIds.isEmpty()) { player.sendMessage(prefix() + "§cAucun constructeur dans ta sélection."); return; }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR || !hand.getType().isBlock()) {
                player.sendMessage(prefix() + "§cTiens le bloc voulu en main avant de cliquer ici.");
                return;
            }
            Material type = hand.getType();
            player.closeInventory();
            villagerManager.startGroupTaskZoneSelection(player, builderIds, type);
            return;
        }

        if (clicked.getType() == Material.CAMPFIRE) {
            if (selected.isEmpty()) { player.sendMessage(prefix() + "§cAucun villageois dans ta sélection."); return; }
            player.closeInventory();
            villagerManager.startGroupRallyPointSelection(player, new ArrayList<>(selected));
            return;
        }
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
            handleFixedSlotClick(player, rv, slot, holder);
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

    private void handleFixedSlotClick(Player player, RecruitedVillager rv, int slot, DetailHolder holder) {
        Faction faction = factionManager.getFaction(rv.getFactionName());
        boolean canManage = faction != null && faction.canManage(player.getUniqueId());
        boolean isBuilder = rv.getRole() == VillagerRole.CONSTRUCTEUR;
        boolean isWarrior = rv.getRole() == VillagerRole.GUERRIER;
        boolean isHarvester = rv.getRole() == VillagerRole.RECOLTEUR;

        switch (slot) {
            case 0 -> { if (canManage) { villagerManager.setRole(rv, VillagerRole.AUCUN); openDetail(player, rv); } else denyManage(player); }
            case 1 -> { if (canManage) { villagerManager.setRole(rv, VillagerRole.CONSTRUCTEUR); openDetail(player, rv); } else denyManage(player); }
            case 2 -> { if (canManage) { villagerManager.setRole(rv, VillagerRole.GUERRIER); openDetail(player, rv); } else denyManage(player); }
            case 3 -> { if (canManage) { villagerManager.setRole(rv, VillagerRole.RECOLTEUR); openDetail(player, rv); } else denyManage(player); }
            case 8 -> {
                if (canManage) {
                    pendingChat.put(player.getUniqueId(), new PendingChatInput(rv.getEntityId(), PendingType.RENAME));
                    player.closeInventory();
                    player.sendMessage(prefix() + "§eTape le nouveau nom de ce villageois dans le chat (ou §cannuler§e).");
                } else denyManage(player);
            }

            // ── Constructeur / Récolteur ──────────────────────────────────
            case 33 -> {
                if (isBuilder) {
                    if (!canManage) { denyManage(player); return; }
                    ItemStack typeItem = holder.inv.getItem(27);
                    if (typeItem == null || typeItem.getType() == Material.AIR || !typeItem.getType().isBlock()) {
                        player.sendMessage(prefix() + "§cPlace d'abord un bloc dans l'emplacement au-dessus (type de bloc voulu).");
                        return;
                    }
                    Material type = typeItem.getType();
                    player.closeInventory();
                    villagerManager.startTaskZoneSelection(player, rv, type);
                } else if (isHarvester) {
                    if (!canManage) { denyManage(player); return; }
                    player.closeInventory();
                    villagerManager.startHarvestZoneSelection(player, rv);
                }
            }
            case 34 -> {
                if (isHarvester && rv.hasHarvestZone()) {
                    if (!canManage) { denyManage(player); return; }
                    villagerManager.clearHarvestZone(rv);
                    player.sendMessage(prefix() + "§aZone de récolte retirée.");
                    openDetail(player, rv);
                }
            }
            case 36 -> {
                if (isWarrior) {
                    if (!canManage) { denyManage(player); return; }
                    Entity e = Bukkit.getEntity(rv.getEntityId());
                    if (e == null) { player.sendMessage(prefix() + "§cVillageois introuvable (chunk non chargé)."); return; }
                    villagerManager.setPost(rv, e.getLocation());
                    player.sendMessage(prefix() + "§a✔ Poste défini à sa position actuelle.");
                    openDetail(player, rv);
                } else if (isHarvester) {
                    if (!canManage) { denyManage(player); return; }
                    player.closeInventory();
                    villagerManager.startFarmZoneSelection(player, rv);
                }
                // Pour le constructeur, slot 36 = info seule (pas d'action)
            }
            case 37 -> {
                if (isWarrior) {
                    if (!canManage) { denyManage(player); return; }
                    pendingChat.put(player.getUniqueId(), new PendingChatInput(rv.getEntityId(), PendingType.RADIUS));
                    player.closeInventory();
                    player.sendMessage(prefix() + "§eTape le rayon de défense en blocs (4-48) dans le chat (ou §cannuler§e).");
                } else if (isBuilder) {
                    if (!canManage) { denyManage(player); return; }
                    if (rv.getCurrentTask() == null) { player.sendMessage(prefix() + "§cAucun chantier actif à annuler."); return; }
                    villagerManager.cancelCurrentTask(rv);
                    player.sendMessage(prefix() + "§aChantier actuel annulé.");
                    openDetail(player, rv);
                } else if (isHarvester && rv.hasFarmZone()) {
                    if (!canManage) { denyManage(player); return; }
                    villagerManager.clearFarmZone(rv);
                    player.sendMessage(prefix() + "§aChamp retiré.");
                    openDetail(player, rv);
                }
            }
            case 38 -> {
                if (isWarrior) {
                    if (!canManage) { denyManage(player); return; }
                    player.closeInventory();
                    villagerManager.startPatrolSelection(player, rv);
                } else if (isBuilder) {
                    if (!canManage) { denyManage(player); return; }
                    if (rv.getTaskQueue().isEmpty()) { player.sendMessage(prefix() + "§cLa file est déjà vide."); return; }
                    villagerManager.clearTaskQueue(rv);
                    player.sendMessage(prefix() + "§aFile de chantiers vidée.");
                    openDetail(player, rv);
                }
            }
            case 39 -> {
                if (isWarrior) {
                    if (!canManage) { denyManage(player); return; }
                    villagerManager.setCombatEnabled(rv, !rv.isCombatEnabled());
                    openDetail(player, rv);
                } else if (isHarvester) {
                    if (!canManage) { denyManage(player); return; }
                    player.closeInventory();
                    villagerManager.startOutputChestSelection(player, rv);
                }
            }
            case 40 -> {
                if (isBuilder) {
                    if (!canManage) { denyManage(player); return; }
                    player.closeInventory();
                    villagerManager.startGatherZoneSelection(player, rv);
                } else if (isHarvester && rv.getOutputChest() != null) {
                    if (!canManage) { denyManage(player); return; }
                    villagerManager.clearOutputChest(rv);
                    player.sendMessage(prefix() + "§aCoffre de dépôt retiré.");
                    openDetail(player, rv);
                }
            }
            case 41 -> {
                if (isBuilder) {
                    if (!canManage) { denyManage(player); return; }
                    if (!rv.hasGatherZone()) return;
                    villagerManager.clearGatherZone(rv);
                    player.sendMessage(prefix() + "§aZone de récolte retirée.");
                    openDetail(player, rv);
                } else if (isWarrior) {
                    villagerManager.toggleFollow(rv, player);
                    openDetail(player, rv);
                }
            }
            case 42 -> {
                if (isWarrior) {
                    if (!canManage) { denyManage(player); return; }
                    villagerManager.setArcheryMode(rv, !rv.isArcheryMode());
                    openDetail(player, rv);
                }
            }
            case 43 -> {
                if (isBuilder || isWarrior || isHarvester) {
                    if (!canManage) { denyManage(player); return; }
                    player.closeInventory();
                    villagerManager.startRallyPointSelection(player, rv);
                }
            }
            case 44 -> {
                if ((isBuilder || isWarrior || isHarvester) && rv.getRallyPoint() != null) {
                    if (!canManage) { denyManage(player); return; }
                    villagerManager.clearRallyPoint(rv);
                    player.sendMessage(prefix() + "§aPoint de rassemblement retiré.");
                    openDetail(player, rv);
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
                case RESOURCE, LOOT -> {
                    Integer idx = holder.resourceIndex.get(slot);
                    if (idx != null) rv.getResources()[idx] = clean;
                }
                case TASK_TYPE -> { /* emplacement transitoire : lu directement au clic "Ajouter un chantier" */ }
                case WEAPON -> rv.setWeapon(clean);
                case HELMET -> rv.setHelmet(clean);
                case CHESTPLATE -> rv.setChestplate(clean);
                case LEGGINGS -> rv.setLeggings(clean);
                case BOOTS -> rv.setBoots(clean);
                case BOW -> rv.setBow(clean);
                case ARROWS -> rv.setArrows(clean);
                case TOOL -> rv.setTool(clean);
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
        PendingChatInput pending = pendingChat.get(player.getUniqueId());
        if (pending == null) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();
        pendingChat.remove(player.getUniqueId());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (msg.equalsIgnoreCase("annuler")) { player.sendMessage(prefix() + "§7Annulé."); return; }
            RecruitedVillager rv = villagerManager.getByEntity(pending.villagerId);
            if (rv == null) { player.sendMessage(prefix() + "§cVillageois introuvable."); return; }

            switch (pending.type) {
                case RENAME -> {
                    String clean = ChatColor.stripColor(msg);
                    if (clean.length() > 24) clean = clean.substring(0, 24);
                    villagerManager.rename(rv, clean);
                    player.sendMessage(prefix() + "§a✔ Renommé en §e" + clean + "§a.");
                }
                case RADIUS -> {
                    try {
                        double r = Double.parseDouble(msg.replace(",", "."));
                        if (r < 4 || r > 48) { player.sendMessage(prefix() + "§cLe rayon doit être entre 4 et 48."); return; }
                        villagerManager.setDefenseRadius(rv, r);
                        player.sendMessage(prefix() + "§a✔ Rayon de défense réglé à §e" + (int) r + " blocs§a.");
                    } catch (NumberFormatException ex) {
                        player.sendMessage(prefix() + "§cNombre invalide.");
                    }
                }
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // VALIDATION D'OBJETS PAR EMPLACEMENT
    // ════════════════════════════════════════════════════════════════════════

    private boolean isValidForSlot(SlotKind kind, Material type) {
        String name = type.name();
        return switch (kind) {
            case RESOURCE -> type != Material.AIR && type.isBlock();
            case TASK_TYPE -> type != Material.AIR && type.isBlock();
            case WEAPON -> name.endsWith("_SWORD") || (name.endsWith("_AXE") && !name.contains("PICK")) || type == Material.TRIDENT;
            case HELMET -> name.endsWith("_HELMET") || type == Material.TURTLE_HELMET;
            case CHESTPLATE -> name.endsWith("_CHESTPLATE") || type == Material.ELYTRA;
            case LEGGINGS -> name.endsWith("_LEGGINGS");
            case BOOTS -> name.endsWith("_BOOTS");
            case BOW -> type == Material.BOW || type == Material.CROSSBOW;
            case ARROWS -> name.endsWith("ARROW");
            case TOOL -> name.endsWith("_PICKAXE") || (name.endsWith("_AXE") && !name.contains("PICK")) || name.endsWith("_SHOVEL");
            case LOOT -> true; // butin/graines : accepte n'importe quel objet
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
            case RECOLTEUR -> "§a";
            default -> "§7";
        };
    }

    private String prettyMaterial(Material m) {
        return m.name().toLowerCase().replace('_', ' ');
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
