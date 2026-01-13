package cn.clexus.mythicMobsAddon.addons.placeholders;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.core.skills.placeholders.types.EntityPlaceholder;

public class TargetTicksLivedPlaceholder implements EntityPlaceholder {
    @Override
    public String apply(AbstractEntity e, String s) {
        return e.getBukkitEntity().getTicksLived() + "";
    }
}
