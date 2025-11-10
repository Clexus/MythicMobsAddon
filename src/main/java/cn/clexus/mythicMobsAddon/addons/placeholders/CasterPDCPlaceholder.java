package cn.clexus.mythicMobsAddon.addons.placeholders;

import cn.clexus.mythicMobsAddon.utils.PDCUtil;
import io.lumine.mythic.core.skills.placeholders.PlaceholderMeta;
import io.lumine.mythic.core.skills.placeholders.types.MetaPlaceholder;

public class CasterPDCPlaceholder implements MetaPlaceholder {
    @Override
    public String apply(PlaceholderMeta placeholderMeta, String s) {
        return PDCUtil.getPDCData(placeholderMeta.getCaster().getEntity().getBukkitEntity(), s);
    }
}
