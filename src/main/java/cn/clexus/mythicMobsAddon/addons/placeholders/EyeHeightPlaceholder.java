package cn.clexus.mythicMobsAddon.addons.placeholders;

import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.placeholders.types.EntityScopedPlaceholder;
import io.lumine.mythic.core.skills.placeholders.types.NumericEntityScopedPlaceholder;
import io.lumine.mythic.core.utils.annotations.MythicPlaceholder;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@MythicPlaceholder(placeholder = "eyeheight")
public class EyeHeightPlaceholder extends NumericEntityScopedPlaceholder {

    public EyeHeightPlaceholder(EntityScopedPlaceholder.EntityScopedPlaceholderArguments context) {
        super(context);
    }

    @Override
    public @Nullable Number applyToScopeWithNumericFormatting(PlaceholderContext placeholderContext) {
        var entity = getEntity.get(placeholderContext);
        if (entity == null) {
            return 0;
        }
        var bukkitEntity = entity.getBukkitEntity();
        if (!(bukkitEntity instanceof LivingEntity livingEntity)) {
            return 0;
        }
        return livingEntity.getEyeHeight();
    }
}


