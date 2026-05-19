package cn.clexus.mythicMobsAddon.addons.placeholders;

import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.placeholders.types.EntityScopedPlaceholder;
import io.lumine.mythic.core.skills.placeholders.types.NumericEntityScopedPlaceholder;
import io.lumine.mythic.core.utils.annotations.MythicPlaceholder;
import org.jspecify.annotations.Nullable;

@MythicPlaceholder(placeholder = "height")
public class HeightPlaceholder extends NumericEntityScopedPlaceholder {

    public HeightPlaceholder(EntityScopedPlaceholder.EntityScopedPlaceholderArguments context) {
        super(context);
    }

    @Override
    public @Nullable Number applyToScopeWithNumericFormatting(PlaceholderContext placeholderContext) {
        var entity = getEntity.get(placeholderContext);
        if (entity == null) {
            return 0D;
        }
        var bukkitEntity = entity.getBukkitEntity();
        return bukkitEntity.getHeight();
    }
}


