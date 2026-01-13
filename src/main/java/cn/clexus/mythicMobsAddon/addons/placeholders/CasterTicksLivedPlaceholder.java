package cn.clexus.mythicMobsAddon.addons.placeholders;

import io.lumine.mythic.core.skills.placeholders.PlaceholderMeta;
import io.lumine.mythic.core.skills.placeholders.types.MetaPlaceholder;

public class CasterTicksLivedPlaceholder implements MetaPlaceholder {
    @Override
    public String apply(PlaceholderMeta placeholderMeta, String s) {
        return placeholderMeta.getCaster().getEntity().getBukkitEntity().getTicksLived() + "";
    }
}
