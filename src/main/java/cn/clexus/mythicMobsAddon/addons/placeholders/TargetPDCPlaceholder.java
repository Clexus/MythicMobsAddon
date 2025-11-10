package cn.clexus.mythicMobsAddon.addons.placeholders;

import cn.clexus.mythicMobsAddon.utils.PDCUtil;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.core.skills.placeholders.types.EntityPlaceholder;

public class TargetPDCPlaceholder implements EntityPlaceholder {

    @Override
    public String apply(AbstractEntity abstractEntity, String s) {
        return PDCUtil.getPDCData(abstractEntity.getBukkitEntity(), s);
    }
}
