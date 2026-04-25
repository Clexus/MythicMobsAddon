package cn.clexus.mythicMobsAddon.addons.mechanics;

import cn.clexus.mythicMobsAddon.MythicMobsAddon;
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
import io.lumine.mythic.core.logging.MythicLogger;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TempAttributeModifierMechanic extends SkillMechanic implements ITargetedEntitySkill, INoTargetSkill {
    private static final Map<ModifierTaskKey, BukkitTask> REMOVAL_TASKS = new ConcurrentHashMap<>();

    private final Attribute attribute;
    private final AttributeModifier.Operation operation;
    private final PlaceholderString name;
    private final PlaceholderDouble amount;
    private final PlaceholderInt duration;

    private record ModifierTaskKey(UUID entityId, Attribute attribute, NamespacedKey modifierKey) {
    }

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
        if (!(entity instanceof Entity bukkitEntity)) {
            return SkillResult.INVALID_TARGET;
        }

        PlaceholderContext context = PlaceholderContext.builder().meta(data).entity(data.getCaster().getEntity()).build();
        NamespacedKey key;
        var name = this.name.get(context);
        if (name.contains(":")) {
            key = NamespacedKey.fromString(name);
        } else {
            key = NamespacedKey.fromString("mmaddon:" + name);
        }
        if (key == null) return SkillResult.INVALID_CONFIG;
        var modifier = new AttributeModifier(key, this.amount.get(context), this.operation);
        AttributeInstance instance = entity.getAttribute(this.attribute);
        if (instance == null) return SkillResult.INVALID_TARGET;

        ModifierTaskKey taskKey = new ModifierTaskKey(bukkitEntity.getUniqueId(), this.attribute, key);
        cancelRemovalTask(taskKey);

        if (instance.getModifier(key) != null) {
            instance.removeModifier(key);
        }
        instance.addTransientModifier(modifier);

        int duration = this.duration.get(context);
        if (duration > 0) {
            final BukkitTask[] taskRef = new BukkitTask[1];
            taskRef[0] = Bukkit.getScheduler().runTaskLater(MythicMobsAddon.plugin, () -> {
                if (REMOVAL_TASKS.get(taskKey) != taskRef[0]) {
                    return;
                }

                REMOVAL_TASKS.remove(taskKey);
                instance.removeModifier(key);
            }, duration);
            REMOVAL_TASKS.put(taskKey, taskRef[0]);
        }

        return SkillResult.SUCCESS;
    }

    private static void cancelRemovalTask(ModifierTaskKey taskKey) {
        BukkitTask previous = REMOVAL_TASKS.remove(taskKey);
        if (previous != null) {
            previous.cancel();
        }
    }

    public static void clearScheduledRemovals() {
        REMOVAL_TASKS.values().forEach(BukkitTask::cancel);
        REMOVAL_TASKS.clear();
    }
}
