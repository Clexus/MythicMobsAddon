package cn.clexus.mythicMobsAddon.addons.placeholders;

import cn.clexus.mythicMobsAddon.utils.PDCUtil;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.placeholders.segments.types.ResolvedPlaceholderSegment;
import io.lumine.mythic.core.skills.placeholders.types.EntityScopedPlaceholder;
import io.lumine.mythic.core.skills.placeholders.types.GenericPlaceholderTypes.StringPlaceholder;
import io.lumine.mythic.core.utils.annotations.MythicPlaceholder;
import org.jetbrains.annotations.Nullable;

@MythicPlaceholder(placeholder = "pdc", usedPlaceholderArguments = 1)
public class PDCPlaceholder extends EntityScopedPlaceholder<String> implements StringPlaceholder {

    private final ResolvedPlaceholderSegment<PlaceholderString> key;

    public PDCPlaceholder(EntityScopedPlaceholder.EntityScopedPlaceholderArguments context) {
        super(context);
        this.key = getPlaceholderString(0);
    }

    @Nullable
    @Override
    public String applyToScope(PlaceholderContext placeholderContext) {
        var entity = getEntity.get(placeholderContext);
        if (entity == null) {
            return null;
        }
        String pdcKey = key.get(placeholderContext, PlaceholderString::get);
        if (pdcKey == null) {
            pdcKey = "";
        }
        return PDCUtil.getPDCData(entity.getBukkitEntity(), pdcKey);
    }
}



