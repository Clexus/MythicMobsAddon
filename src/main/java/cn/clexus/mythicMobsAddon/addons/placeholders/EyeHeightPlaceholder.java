package cn.clexus.mythicMobsAddon.addons.placeholders;

import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.placeholders.types.EntityScopedPlaceholder;
import io.lumine.mythic.core.skills.placeholders.types.GenericPlaceholderTypes.StringPlaceholder;
import io.lumine.mythic.core.utils.annotations.MythicPlaceholder;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@MythicPlaceholder(placeholder = "eyeheight")
public class EyeHeightPlaceholder extends EntityScopedPlaceholder<String> implements StringPlaceholder {

    public EyeHeightPlaceholder(EntityScopedPlaceholder.EntityScopedPlaceholderArguments context) {
        super(context);
    }

    @Nullable
    @Override
    public String applyToScope(PlaceholderContext placeholderContext) {
        var entity = getEntity.get(placeholderContext);
        if (entity == null) {
            return "0";
        }
        var bukkitEntity = entity.getBukkitEntity();
        if (!(bukkitEntity instanceof LivingEntity livingEntity)) {
            return "0";
        }
        return String.valueOf(livingEntity.getEyeHeight());
    }
}


