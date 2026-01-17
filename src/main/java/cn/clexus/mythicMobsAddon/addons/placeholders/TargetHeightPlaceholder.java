package cn.clexus.mythicMobsAddon.addons.placeholders;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.core.skills.placeholders.types.EntityPlaceholder;
import org.bukkit.entity.LivingEntity;

public class TargetHeightPlaceholder implements EntityPlaceholder {
    @Override
    public String apply(AbstractEntity abstractEntity, String string) {
        var e = abstractEntity.getBukkitEntity();
        if(!(e instanceof LivingEntity l)) return "0";
        return String.valueOf(l.getHeight());
    }
}
