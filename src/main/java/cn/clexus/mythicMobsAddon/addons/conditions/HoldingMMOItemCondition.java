package cn.clexus.mythicMobsAddon.addons.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.core.skills.SkillCondition;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.entity.LivingEntity;

public class HoldingMMOItemCondition extends SkillCondition implements IEntityCondition {
    private final String id;

    public HoldingMMOItemCondition(MythicLineConfig mlc) {
        super(mlc.getLine());
        this.id = mlc.getString("id");
    }

    @Override
    public boolean check(AbstractEntity target) {
        if(id == null || id.isEmpty()) return false;
        if(!(target.getBukkitEntity() instanceof LivingEntity living)) return false;
        if(living.getEquipment()==null) return false;
        var item = living.getEquipment().getItemInMainHand();
        if(item.isEmpty()) return false;
        var nbtItem = NBTItem.get(item);
        if(nbtItem.getType() == null) return false;
        return id.equalsIgnoreCase(nbtItem.getString("MMOITEMS_ITEM_ID"));
    }
}
