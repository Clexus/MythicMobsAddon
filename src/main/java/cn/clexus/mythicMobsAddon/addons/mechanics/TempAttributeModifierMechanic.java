package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.INoTargetSkill;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.api.skills.placeholders.PlaceholderInt;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.adapters.BukkitAttribute;
import io.lumine.mythic.bukkit.utils.Schedulers;
import io.lumine.mythic.core.logging.MythicLogger;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class TempAttributeModifierMechanic extends SkillMechanic implements ITargetedEntitySkill, INoTargetSkill {
    private final Attribute attribute;
    private final AttributeModifier.Operation operation;
    private final PlaceholderString name;
    private final PlaceholderDouble amount;
    private final PlaceholderInt duration;

    public TempAttributeModifierMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);
        this.attribute = BukkitAttribute.getAttribute(mlc.getString(new String[]{"attribute", "attr"}));
        if (this.attribute == null) {
            MythicLogger.errorGenericConfig("Not a valid attribute given");
        }

        this.operation = mlc.getEnum(new String[]{"operation", "op"}, AttributeModifier.Operation.class, AttributeModifier.Operation.ADD_NUMBER, "Not a valid operation");
        this.name = mlc.getPlaceholderString(new String[]{"name", "modifierName"}, "<caster.uuid>");
        this.amount = mlc.getPlaceholderDouble(new String[]{"amount", "amt", "a"}, 0.0F);
        this.duration = mlc.getPlaceholderInteger(new String[]{"duration", "dur"}, 0);
    }


    public SkillResult cast(SkillMetadata data) {
        if (this.attribute == null) {
            return SkillResult.INVALID_CONFIG;
        } else {
            if (data.getCaster().getEntity().getBukkitEntity() instanceof Attributable entity) {
                return addAttribute(data, entity);
            } else {
                return SkillResult.INVALID_TARGET;
            }
        }
    }

    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        Entity var4 = target.getBukkitEntity();
        if (var4 instanceof Attributable entity) {
            return addAttribute(data, entity);
        } else {
            return SkillResult.INVALID_TARGET;
        }
    }

    @NotNull
    private SkillResult addAttribute(SkillMetadata data, Attributable entity) {
        var modifier = new AttributeModifier(NamespacedKey.fromString("mmaddon:"+this.name.get(data)), this.amount.get(data), this.operation);
        AttributeInstance instance = entity.getAttribute(this.attribute);
        if(instance == null) return SkillResult.INVALID_TARGET;
        instance.addTransientModifier(modifier);
        int duration = this.duration.get(data);
        if (duration > 0) {
            Schedulers.sync().runLater(() -> instance.removeModifier(modifier), duration);
        }
        return SkillResult.SUCCESS;
    }
}
