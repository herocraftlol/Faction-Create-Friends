package fr.faction.commerce;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import fr.faction.village.PostType;
import fr.faction.village.Village;
import fr.faction.village.VillageManager;
import fr.faction.villager.RecruitedVillager;
import fr.faction.villager.VillagerManager;
import fr.faction.villager.VillagerRole;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Commerce inter-villes : contrats de livraison entre ports (bateaux) ou gares (trains),
 * réservé aux villages au palier "Ville" (niveau 5+). Le fret n'est accessible qu'aux
 * membres de la faction propriétaire ou d'une faction alliée avec qui elle échange.
 *
 * (Sans rapport avec fr.faction.trade.TradeManager, qui gère le troc joueur-à-joueur.)
 *
 * Simplifications assumées (projet déjà très vaste) :
 * - Un contrat = une livraison à sens unique. Pour un "aller-retour", crée un second
 *   contrat dans l'autre sens.
 * - Le trajet est une ligne droite entre le port/la gare de départ et celui d'arrivée,
 *   parcourue par petits à-coups de vitesse (pas d'évitement d'obstacles, pas de suivi
 *   fin des rails) : prévois un chenal ou une voie dégagée entre les deux villes.
 */
public class CommerceManager implements Listener {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private VillageManager villageManager;
    private VillagerManager villagerManager;

    private final Map<UUID, Contract> contracts = new HashMap<>();
    private final File dataFile;
    private final NamespacedKey ownerFactionKey;

    public CommerceManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.dataFile = new File(plugin.getDataFolder(), "contracts.yml");
        this.ownerFactionKey = new NamespacedKey(plugin, "trade_owner_faction");
        load();
    }

    public void setVillageManager(VillageManager vm)   { this.villageManager = vm; }
    public void setVillagerManager(VillagerManager vm) { this.villagerManager = vm; }

    // ════════════════════════════════════════════════════════════════════════
    // ACCESSEURS
    // ════════════════════════════════════════════════════════════════════════

    public Contract getById(UUID id) { return contracts.get(id); }

    public List<Contract> getFactionContracts(String factionName) {
        List<Contract> list = new ArrayList<>();
        for (Contract c : contracts.values()) {
            if (c.getFromFaction().equalsIgnoreCase(factionName) || c.getToFaction().equalsIgnoreCase(factionName)) list.add(c);
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════════════════
    // CRÉATION DE CONTRAT
    // ════════════════════════════════════════════════════════════════════════

    public record CreateResult(boolean success, String message, Contract contract) {}

    public CreateResult createContract(Player player, String fromVillageName, PostType type,
                                        String toFactionName, String toVillageName,
                                        Material resource, int quantity) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return new CreateResult(false, "Tu n'es pas dans une faction.", null);
        if (!faction.canManage(player.getUniqueId())) return new CreateResult(false, "Seul le chef ou un sous-chef peut créer un contrat.", null);
        if (quantity <= 0) return new CreateResult(false, "La quantité doit être positive.", null);
        if (villageManager == null) return new CreateResult(false, "Système de villages non disponible.", null);

        Village fromVillage = villageManager.getByName(faction.getName(), fromVillageName);
        if (fromVillage == null) return new CreateResult(false, "Aucun village de ta faction ne s'appelle " + fromVillageName + ".", null);
        if (!hasPostType(fromVillage, type)) return new CreateResult(false, fromVillage.getName() + " n'a pas de " + type.displayName().toLowerCase() + " défini(e).", null);
        if (!fromVillage.canTrade(villageManager.getLevel(fromVillage))) {
            return new CreateResult(false, fromVillage.getName() + " doit atteindre le palier Ville (niveau 5) pour commercer.", null);
        }

        boolean sameFaction = faction.getName().equalsIgnoreCase(toFactionName);
        if (!sameFaction && !faction.isAlly(toFactionName)) {
            return new CreateResult(false, "Ta faction doit être alliée de " + toFactionName + " pour commercer avec elle.", null);
        }

        Village toVillage = villageManager.getByName(toFactionName, toVillageName);
        if (toVillage == null) return new CreateResult(false, "Aucun village de " + toFactionName + " ne s'appelle " + toVillageName + ".", null);
        if (!hasPostType(toVillage, type)) return new CreateResult(false, toVillage.getName() + " n'a pas de " + type.displayName().toLowerCase() + " défini(e).", null);
        if (!toVillage.canTrade(villageManager.getLevel(toVillage))) {
            return new CreateResult(false, toVillage.getName() + " n'a pas encore atteint le palier Ville (niveau 5).", null);
        }

        Contract contract = new Contract(UUID.randomUUID(), faction.getName(), fromVillage.getName(), type,
                toFactionName, toVillage.getName(), resource, quantity);
        contracts.put(contract.getId(), contract);
        save();
        return new CreateResult(true, "Contrat créé : " + quantity + " " + prettyMaterial(resource)
                + " de " + fromVillage.getName() + " vers " + toVillage.getName() + ".", contract);
    }

    public boolean cancelContract(Player player, Contract contract) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null || !faction.getName().equalsIgnoreCase(contract.getFromFaction())) return false;
        if (!faction.canManage(player.getUniqueId())) return false;
        if (contract.getStatus() == Contract.Status.IN_TRANSIT) return false; // laisser terminer le trajet en cours
        contract.setStatus(Contract.Status.CANCELLED);
        save();
        return true;
    }

    private boolean hasPostType(Village v, PostType type) {
        return type == PostType.PORT ? v.hasPort() : v.hasStation();
    }

    // ════════════════════════════════════════════════════════════════════════
    // ASSIGNATION (départ effectif du trajet)
    // ════════════════════════════════════════════════════════════════════════

    public enum AssignResult { SUCCESS, WRONG_ROLE, ALREADY_BUSY, NOT_WAITING, WRONG_FACTION, NO_WAREHOUSE, NOT_ENOUGH_CARGO, NO_DEST_POST }

    public AssignResult assignContract(RecruitedVillager rv, Contract contract) {
        if (!rv.getFactionName().equalsIgnoreCase(contract.getFromFaction())) return AssignResult.WRONG_FACTION;
        if (contract.getStatus() != Contract.Status.WAITING) return AssignResult.NOT_WAITING;
        if (rv.getContractId() != null) return AssignResult.ALREADY_BUSY;
        boolean roleOk = (contract.getPostType() == PostType.PORT && rv.getRole() == VillagerRole.NAVIGATEUR)
                || (contract.getPostType() == PostType.GARE && rv.getRole() == VillagerRole.CHEMINOT);
        if (!roleOk) return AssignResult.WRONG_ROLE;

        Village fromVillage = villageManager.getByName(contract.getFromFaction(), contract.getFromVillage());
        Village toVillage = villageManager.getByName(contract.getToFaction(), contract.getToVillage());
        if (fromVillage == null || toVillage == null) return AssignResult.NO_DEST_POST;
        Location fromLoc = contract.getPostType() == PostType.PORT ? fromVillage.getPort() : fromVillage.getStation();
        Location toLoc = contract.getPostType() == PostType.PORT ? toVillage.getPort() : toVillage.getStation();
        if (fromLoc == null || toLoc == null) return AssignResult.NO_DEST_POST;

        if (!(fromLoc.getBlock().getState() instanceof Container warehouse)) return AssignResult.NO_WAREHOUSE;
        if (countMatching(warehouse.getInventory(), contract.getResource()) < contract.getQuantity()) return AssignResult.NOT_ENOUGH_CARGO;
        removeMatching(warehouse.getInventory(), contract.getResource(), contract.getQuantity());

        ItemStack cargo = new ItemStack(contract.getResource(), contract.getQuantity());
        EntityType vehicleType = contract.getPostType() == PostType.PORT ? EntityType.OAK_CHEST_BOAT : EntityType.CHEST_MINECART;
        Entity vehicle = spawnCargoVehicle(contract.getFromFaction(), fromLoc, vehicleType, cargo);

        Entity ve = Bukkit.getEntity(rv.getEntityId());
        if (ve instanceof Villager v) vehicle.addPassenger(v);

        rv.setContractId(contract.getId());
        rv.setTransitWaypoints(buildWaypoints(fromLoc, toLoc));
        rv.setWaypointIndex(0);
        rv.setVehicleId(vehicle.getUniqueId());
        rv.setReturningTrip(false);

        contract.setStatus(Contract.Status.IN_TRANSIT);
        contract.setAssignedVillagerId(rv.getEntityId());
        save();
        return AssignResult.SUCCESS;
    }

    // ════════════════════════════════════════════════════════════════════════
    // DÉPLACEMENT (appelé depuis VillagerManager pour les rôles Navigateur/Cheminot)
    // ════════════════════════════════════════════════════════════════════════

    public void navigateurTick(RecruitedVillager rv, Villager v) { transitTick(rv, v, 0.35); }
    public void cheminotTick(RecruitedVillager rv, Villager v)   { transitTick(rv, v, 0.45); }

    private void transitTick(RecruitedVillager rv, Villager v, double speed) {
        UUID contractId = rv.getContractId();
        if (contractId == null) {
            // Rien en cours : il reste à son poste (ou rentre au village s'il en a un et pas de poste).
            if (rv.getPostLocation() != null) {
                simpleApproach(v, rv.getPostLocation(), 4.0, 0.4);
            } else if (villageManager != null && rv.getVillageName() != null) {
                Village home = villageManager.getByName(rv.getFactionName(), rv.getVillageName());
                if (home != null) simpleApproach(v, home.getCenter(), 4.0, 0.4);
            }
            return;
        }

        Contract contract = contracts.get(contractId);
        List<Location> waypoints = rv.getTransitWaypoints();
        if (contract == null || waypoints == null || waypoints.isEmpty() || rv.getVehicleId() == null) {
            resetLostTransit(rv, contract);
            return;
        }

        Entity vehicle = Bukkit.getEntity(rv.getVehicleId());
        if (vehicle == null || vehicle.isDead()) { resetLostTransit(rv, contract); return; }

        int idx = rv.getWaypointIndex();
        if (idx >= waypoints.size()) {
            if (!rv.isReturningTrip()) {
                depositCargo(contract, vehicle);
                startReturnLeg(rv);
            } else {
                finishContract(rv, contract, vehicle);
            }
            return;
        }

        Location target = waypoints.get(idx);
        Location current = vehicle.getLocation();
        if (!current.getWorld().equals(target.getWorld())) { rv.setWaypointIndex(idx + 1); return; }

        double dist = current.distance(target);
        if (dist < 2.0) {
            rv.setWaypointIndex(idx + 1);
            return;
        }
        Vector dir = target.toVector().subtract(current.toVector());
        dir.setY(Math.max(-0.3, Math.min(0.3, dir.getY())));
        dir.normalize().multiply(speed);
        vehicle.setVelocity(dir);
    }

    private void simpleApproach(Villager v, Location dest, double arriveDistance, double speed) {
        if (!v.getWorld().equals(dest.getWorld())) { v.teleport(dest); return; }
        double dist = v.getLocation().distance(dest);
        if (dist <= arriveDistance) return;
        if (dist > 80.0) { v.teleport(dest); return; }
        v.getPathfinder().moveTo(dest, speed);
    }

    private void resetLostTransit(RecruitedVillager rv, Contract contract) {
        if (contract != null && contract.getStatus() == Contract.Status.IN_TRANSIT) {
            contract.setStatus(Contract.Status.WAITING);
            contract.setAssignedVillagerId(null);
            save();
        }
        rv.setContractId(null);
        rv.setTransitWaypoints(null);
        rv.setVehicleId(null);
        rv.setReturningTrip(false);
    }

    private void startReturnLeg(RecruitedVillager rv) {
        List<Location> reversed = new ArrayList<>(rv.getTransitWaypoints());
        Collections.reverse(reversed);
        rv.setTransitWaypoints(reversed);
        rv.setWaypointIndex(0);
        rv.setReturningTrip(true);
    }

    private void finishContract(RecruitedVillager rv, Contract contract, Entity vehicle) {
        contract.setStatus(Contract.Status.DELIVERED);
        vehicle.eject();
        vehicle.remove();
        notifyFaction(contract.getFromFaction(), "§7[Commerce] §f" + rv.getDisplayName()
                + " est rentré après avoir livré le contrat vers §e" + contract.getToVillage() + "§f.");
        rv.setContractId(null);
        rv.setTransitWaypoints(null);
        rv.setVehicleId(null);
        rv.setReturningTrip(false);
        save();
    }

    private void depositCargo(Contract contract, Entity vehicle) {
        if (!(vehicle instanceof InventoryHolder holder)) return;
        Village toVillage = villageManager.getByName(contract.getToFaction(), contract.getToVillage());
        if (toVillage == null) return;
        Location destLoc = contract.getPostType() == PostType.PORT ? toVillage.getPort() : toVillage.getStation();
        if (destLoc == null) return;

        if (destLoc.getBlock().getState() instanceof Container warehouse) {
            Inventory inv = holder.getInventory();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item == null || item.getType() == Material.AIR) continue;
                Map<Integer, ItemStack> leftover = warehouse.getInventory().addItem(item.clone());
                for (ItemStack extra : leftover.values()) destLoc.getWorld().dropItemNaturally(destLoc, extra);
                inv.setItem(i, null);
            }
        }
        notifyFaction(contract.getToFaction(), "§7[Commerce] §fLivraison reçue à §e" + toVillage.getName()
                + "§f : §e" + contract.getQuantity() + " " + prettyMaterial(contract.getResource())
                + " §fen provenance de §e" + contract.getFromFaction() + "§f.");
    }

    private Entity spawnCargoVehicle(String ownerFaction, Location at, EntityType type, ItemStack cargo) {
        Entity vehicle = at.getWorld().spawnEntity(at, type);
        if (vehicle instanceof InventoryHolder holder) {
            holder.getInventory().addItem(cargo);
        }
        vehicle.getPersistentDataContainer().set(ownerFactionKey, PersistentDataType.STRING, ownerFaction);
        vehicle.setPersistent(true);
        return vehicle;
    }

    private List<Location> buildWaypoints(Location from, Location to) {
        List<Location> points = new ArrayList<>();
        if (!from.getWorld().equals(to.getWorld())) { points.add(to.clone()); return points; }
        double dist = from.distance(to);
        int steps = Math.max(1, (int) (dist / 6));
        Vector delta = to.toVector().subtract(from.toVector());
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            points.add(from.clone().add(delta.clone().multiply(t)));
        }
        return points;
    }

    // ════════════════════════════════════════════════════════════════════════
    // PROTECTION DU FRET : réservé à la faction propriétaire et ses alliés
    // ════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof Entity vehicle)) return;
        String owner = vehicle.getPersistentDataContainer().get(ownerFactionKey, PersistentDataType.STRING);
        if (owner == null) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        if (isAuthorized(player, owner)) return;
        event.setCancelled(true);
        player.sendMessage(prefix() + "§cCe chargement appartient à une autre faction.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        String owner = event.getVehicle().getPersistentDataContainer().get(ownerFactionKey, PersistentDataType.STRING);
        if (owner == null) return;
        if (event.getEntered() instanceof Villager) return; // le navigateur/cheminot assigné
        if (!(event.getEntered() instanceof Player player)) { event.setCancelled(true); return; }
        if (isAuthorized(player, owner)) return;
        event.setCancelled(true);
    }

    private boolean isAuthorized(Player player, String ownerFaction) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return false;
        return faction.getName().equalsIgnoreCase(ownerFaction) || faction.isAlly(ownerFaction);
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILS
    // ════════════════════════════════════════════════════════════════════════

    private int countMatching(Inventory inv, Material type) {
        int total = 0;
        for (ItemStack it : inv.getContents()) if (it != null && it.getType() == type) total += it.getAmount();
        return total;
    }

    private void removeMatching(Inventory inv, Material type, int amount) {
        for (int i = 0; i < inv.getSize() && amount > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != type) continue;
            int take = Math.min(it.getAmount(), amount);
            amount -= take;
            int remaining = it.getAmount() - take;
            inv.setItem(i, remaining <= 0 ? null : it);
            if (remaining > 0) it.setAmount(remaining);
        }
    }

    private String prettyMaterial(Material m) {
        return m.name().toLowerCase().replace('_', ' ');
    }

    private void notifyFaction(String factionName, String message) {
        Faction f = factionManager.getFaction(factionName);
        if (f == null) return;
        for (UUID uuid : f.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(prefix() + message);
        }
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.prefix", "&8[&6Faction&8] &r"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // PERSISTANCE
    // ════════════════════════════════════════════════════════════════════════

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        org.bukkit.configuration.file.FileConfiguration cfg = new org.bukkit.configuration.file.YamlConfiguration();
        for (Contract c : contracts.values()) {
            String key = "contracts." + c.getId();
            cfg.set(key + ".fromFaction", c.getFromFaction());
            cfg.set(key + ".fromVillage", c.getFromVillage());
            cfg.set(key + ".postType", c.getPostType().name());
            cfg.set(key + ".toFaction", c.getToFaction());
            cfg.set(key + ".toVillage", c.getToVillage());
            cfg.set(key + ".resource", c.getResource().name());
            cfg.set(key + ".quantity", c.getQuantity());
            cfg.set(key + ".status", c.getStatus().name());
        }
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("Erreur sauvegarde contrats : " + e.getMessage());
        }
    }

    public void load() {
        if (!dataFile.exists()) return;
        var cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
        if (!cfg.contains("contracts")) return;
        var section = cfg.getConfigurationSection("contracts");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String path = "contracts." + key;
            UUID id;
            try { id = UUID.fromString(key); } catch (Exception e) { continue; }
            try {
                String fromFaction = cfg.getString(path + ".fromFaction");
                String fromVillage = cfg.getString(path + ".fromVillage");
                PostType postType = PostType.valueOf(cfg.getString(path + ".postType"));
                String toFaction = cfg.getString(path + ".toFaction");
                String toVillage = cfg.getString(path + ".toVillage");
                Material resource = Material.valueOf(cfg.getString(path + ".resource"));
                int quantity = cfg.getInt(path + ".quantity");
                Contract c = new Contract(id, fromFaction, fromVillage, postType, toFaction, toVillage, resource, quantity);
                String statusStr = cfg.getString(path + ".status", "WAITING");
                Contract.Status status = Contract.Status.valueOf(statusStr);
                c.setStatus(status == Contract.Status.IN_TRANSIT ? Contract.Status.WAITING : status);
                contracts.put(id, c);
            } catch (Exception ignored) { /* entrée corrompue, on l'ignore */ }
        }
        plugin.getLogger().info(contracts.size() + " contrat(s) chargé(s).");
    }
}
