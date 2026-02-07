package cn.clexus.mythicMobsAddon.addons.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.core.skills.SkillCondition;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class HasMMOItemCondition extends SkillCondition implements IEntityCondition {
    private final String id;

    public HasMMOItemCondition(MythicLineConfig mlc) {
        super(mlc.getLine());
        this.id = mlc.getString("id");
    }

    @Override
    public boolean check(AbstractEntity target) {
        if (id == null || id.isEmpty()) return false;
        if (!(target.getBukkitEntity() instanceof LivingEntity living)) return false;
        if (living.getEquipment() == null && !(living instanceof InventoryHolder)) return false;
        for (var item : living.getEquipment().getArmorContents()) {
            if (check(item, id)) return true;
        }
        var item = living.getEquipment().getItemInMainHand();
        if (check(item, id)) return true;
        item = living.getEquipment().getItemInOffHand();
        if (check(item, id)) return true;
        if (living instanceof InventoryHolder holder) {
            var inventory = holder.getInventory();
            for (var invItem : inventory.getContents()) {
                if (invItem != null && check(invItem, id)) return true;
            }
        }
        return false;
    }

    public boolean check(ItemStack item, String id) {
        if (item == null || item.isEmpty()) return false;
        var nbtItem = NBTItem.get(item);
        if (nbtItem.getType() == null) return false;
        return id.equalsIgnoreCase(nbtItem.getString("MMOITEMS_ITEM_ID"));
    }
}