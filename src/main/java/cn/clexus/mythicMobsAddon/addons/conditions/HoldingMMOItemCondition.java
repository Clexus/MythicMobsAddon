package cn.clexus.mythicMobsAddon.addons.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.api.skills.placeholders.PlaceholderBoolean;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.core.skills.SkillCondition;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.entity.LivingEntity;

public class HoldingMMOItemCondition extends SkillCondition implements IEntityCondition {
    private final PlaceholderString id;
    private final PlaceholderBoolean offhand;

    public HoldingMMOItemCondition(MythicLineConfig mlc) {
        super(mlc.getLine());
        this.id = PlaceholderString.of(mlc.getString("id"));
        this.offhand = PlaceholderBoolean.of(mlc.getString("offhand", "false"));
    }

    @Override
    public boolean check(AbstractEntity target) {
        String id = this.id.get(PlaceholderContext.builder().entity(target).build());
        boolean offhand = this.offhand.get(PlaceholderContext.builder().entity(target).build());
        if (id == null || id.isEmpty()) return false;
        if (!(target.getBukkitEntity() instanceof LivingEntity living)) return false;
        if (living.getEquipment() == null) return false;
        var item = offhand ? living.getEquipment().getItemInOffHand() : living.getEquipment().getItemInMainHand();
        if (item.isEmpty()) return false;
        var nbtItem = NBTItem.get(item);
        if (nbtItem.getType() == null) return false;
        return id.equalsIgnoreCase(nbtItem.getString("MMOITEMS_ITEM_ID"));
    }
}
