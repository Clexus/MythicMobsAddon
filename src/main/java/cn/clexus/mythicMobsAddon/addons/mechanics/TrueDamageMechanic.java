package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import org.bukkit.craftbukkit.damage.CraftDamageSource;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.io.File;

public class TrueDamageMechanic extends SkillMechanic implements ITargetedEntitySkill {
    private final PlaceholderDouble damage;

    public TrueDamageMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);

        this.damage = PlaceholderDouble.of(mlc.getString(new String[]{"damage", "amount", "a", "d"}, "0"));
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata skillMetadata, AbstractEntity abstractEntity) {
        if (!(abstractEntity.getBukkitEntity() instanceof LivingEntity l) || !(skillMetadata.getCaster().getEntity().getBukkitEntity() instanceof LivingEntity c))
            return SkillResult.INVALID_TARGET;
        double damage = this.damage.get(skillMetadata, abstractEntity);
        var source = DamageSource.builder(c instanceof Player ? DamageType.PLAYER_ATTACK : DamageType.MOB_ATTACK).withDirectEntity(c).withCausingEntity(c).build();
        CraftLivingEntity cL = (CraftLivingEntity) l;
        var health = (float) Math.clamp(cL.getHealth() - damage, 0, cL.getMaxHealth());
        if (cL.getHandle().generation && health == 0) {
            cL.getHandle().discard(null);
            return SkillResult.INVALID_TARGET;
        }

        if (health == 0) {
            l.setLastDamageCause(new EntityDamageByEntityEvent(c, l, EntityDamageEvent.DamageCause.ENTITY_ATTACK, source, damage));
            cL.getHandle().die(((CraftDamageSource) source).getHandle());
            cL.getHandle().setHealth(health);
        } else {
            cL.getHandle().setHealth(health);
        }

        return SkillResult.SUCCESS;
    }
}
