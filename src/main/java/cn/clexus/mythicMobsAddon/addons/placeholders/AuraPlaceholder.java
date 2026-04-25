package cn.clexus.mythicMobsAddon.addons.placeholders;

import cn.clexus.mythicMobsAddon.utils.AuraUtil;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.placeholders.segments.types.ResolvedPlaceholderSegment;
import io.lumine.mythic.core.skills.placeholders.types.EntityScopedPlaceholder;
import io.lumine.mythic.core.skills.placeholders.types.GenericPlaceholderTypes.StringPlaceholder;
import io.lumine.mythic.core.utils.annotations.MythicPlaceholder;
import org.jetbrains.annotations.Nullable;

@MythicPlaceholder(placeholder = "aura", usedPlaceholderArguments = 1)
public class AuraPlaceholder extends EntityScopedPlaceholder<String> implements StringPlaceholder {

    private final ResolvedPlaceholderSegment<PlaceholderString> arguments;

    public AuraPlaceholder(EntityScopedPlaceholder.EntityScopedPlaceholderArguments context) {
        super(context);
        this.arguments = getPlaceholderString(0);
    }

    @Nullable
    @Override
    public String applyToScope(PlaceholderContext placeholderContext) {
        var entity = getEntity.get(placeholderContext);
        if (entity == null) {
            return null;
        }
        String args = arguments.get(placeholderContext, PlaceholderString::get);
        if (args == null) {
            args = "";
        }
        return AuraUtil.processArgs(entity, args);
    }
}




