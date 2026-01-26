package cn.clexus.mythicMobsAddon.addons.placeholders;

import cn.clexus.mythicMobsAddon.utils.AuraUtil;
import io.lumine.mythic.core.skills.placeholders.PlaceholderMeta;
import io.lumine.mythic.core.skills.placeholders.types.MetaPlaceholder;

public class CasterAuraPlaceholder implements MetaPlaceholder {
    @Override
    public String apply(PlaceholderMeta placeholderMeta, String string) {
        var caster = placeholderMeta.getCaster().getEntity();
        return AuraUtil.processArgs(caster, string);
    }
}
