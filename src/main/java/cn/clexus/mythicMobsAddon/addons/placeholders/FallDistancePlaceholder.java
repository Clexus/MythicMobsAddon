package cn.clexus.mythicMobsAddon.addons.placeholders;

import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.placeholders.types.NumericEntityScopedPlaceholder;
import io.lumine.mythic.core.utils.annotations.MythicPlaceholder;
import org.jetbrains.annotations.Nullable;

@MythicPlaceholder(placeholder = "falldistance")
public class FallDistancePlaceholder extends NumericEntityScopedPlaceholder {

    public FallDistancePlaceholder(EntityScopedPlaceholderArguments context) {
        super(context);
    }

    @Override
    public @Nullable Number applyToScopeWithNumericFormatting(PlaceholderContext placeholderContext) {
        var entity = getEntity.get(placeholderContext);
        if (entity == null) {
            return 0;
        }
        var bukkitEntity = entity.getBukkitEntity();
        return bukkitEntity.getFallDistance();
    }
}


