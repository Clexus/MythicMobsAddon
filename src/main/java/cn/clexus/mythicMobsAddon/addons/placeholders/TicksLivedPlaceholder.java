package cn.clexus.mythicMobsAddon.addons.placeholders;

import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.placeholders.types.EntityScopedPlaceholder;
import io.lumine.mythic.core.skills.placeholders.types.GenericPlaceholderTypes.IntegerPlaceholder;
import io.lumine.mythic.core.utils.annotations.MythicPlaceholder;
import org.jetbrains.annotations.Nullable;

@MythicPlaceholder(placeholder = "tickslived")
public class TicksLivedPlaceholder extends EntityScopedPlaceholder<Integer> implements IntegerPlaceholder {

    public TicksLivedPlaceholder(EntityScopedPlaceholder.EntityScopedPlaceholderArguments context) {
        super(context);
    }

    @Nullable
    @Override
    public Integer applyToScope(PlaceholderContext placeholderContext) {
        var entity = getEntity.get(placeholderContext);
        if (entity == null) {
            return 0;
        }
        return entity.getBukkitEntity().getTicksLived();
    }
}


