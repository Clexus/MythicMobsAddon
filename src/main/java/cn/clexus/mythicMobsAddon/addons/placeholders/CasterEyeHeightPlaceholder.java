package cn.clexus.mythicMobsAddon.addons.placeholders;

import io.lumine.mythic.core.skills.placeholders.PlaceholderMeta;
import io.lumine.mythic.core.skills.placeholders.types.MetaPlaceholder;
import org.bukkit.entity.LivingEntity;

public class CasterEyeHeightPlaceholder implements MetaPlaceholder {
    @Override
    public String apply(PlaceholderMeta placeholderMeta, String string) {
        var e = placeholderMeta.getCaster().getEntity().getBukkitEntity();
        if(!(e instanceof LivingEntity l)) return "0";
        return String.valueOf(l.getEyeHeight());
    }
}
