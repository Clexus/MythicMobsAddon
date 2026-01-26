package cn.clexus.mythicMobsAddon.addons.placeholders;

import cn.clexus.mythicMobsAddon.utils.AuraUtil;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.core.skills.placeholders.types.EntityPlaceholder;

public class TargetAuraPlaceholder implements EntityPlaceholder {

    @Override
    public String apply(AbstractEntity entity, String string) {
        return AuraUtil.processArgs(entity, string);
    }
}
