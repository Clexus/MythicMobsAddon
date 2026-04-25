package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.INoTargetSkill;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.adapters.BukkitAttribute;
import io.lumine.mythic.core.logging.MythicLogger;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class RemoveAttributeModifierMechanic extends SkillMechanic implements ITargetedEntitySkill, INoTargetSkill {
    private final Attribute attribute;
    private final PlaceholderString name;

    public RemoveAttributeModifierMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);
        this.attribute = BukkitAttribute.getAttribute(mlc.getString(new String[]{"attribute", "attr"}));
        if (this.attribute == null) {
            MythicLogger.errorGenericConfig("Not a valid attribute given");
        }
        this.name = mlc.getPlaceholderString(new String[]{"name", "modifierName"}, "<caster.uuid>");
    }


    public SkillResult cast(SkillMetadata data) {
        if (data.getCaster().getEntity().getBukkitEntity() instanceof Attributable entity) {
            return removeAttribute(data, entity);
        } else {
            return SkillResult.INVALID_TARGET;
        }
    }

    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        Entity var4 = target.getBukkitEntity();
        if (var4 instanceof Attributable entity) {
            return removeAttribute(data, entity);
        } else {
            return SkillResult.INVALID_TARGET;
        }
    }

    @NotNull
    private SkillResult removeAttribute(SkillMetadata data, Attributable entity) {
        PlaceholderContext context = PlaceholderContext.builder().meta(data).entity(data.getCaster().getEntity()).build();
        var name = this.name.get(context);
        NamespacedKey key;
        if (name.contains(":")) {
            key = NamespacedKey.fromString(name);
        } else {
            key = NamespacedKey.fromString("mmaddon:" + name);
        }
        if (key == null) return SkillResult.INVALID_CONFIG;
        AttributeInstance instance = entity.getAttribute(this.attribute);
        if (instance == null) return SkillResult.INVALID_TARGET;
        instance.removeModifier(key);
        return SkillResult.SUCCESS;
    }
}