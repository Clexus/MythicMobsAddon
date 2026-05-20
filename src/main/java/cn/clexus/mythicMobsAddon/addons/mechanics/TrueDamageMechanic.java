package cn.clexus.mythicMobsAddon.addons.mechanics;

import cn.clexus.mythicMobsAddon.MythicMobsAddon;
import cn.clexus.mythicMobsAddon.events.EntityTrueDamageEvent;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.ThreadSafetyLevel;
import io.lumine.mythic.api.skills.placeholders.PlaceholderBoolean;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import org.bukkit.Bukkit;
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
    private final PlaceholderBoolean damagePlayer;
    private final PlaceholderBoolean ignoreInvulnerable;
    private final PlaceholderString sound;
    private final PlaceholderString disabledServer;


    public TrueDamageMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);
        this.forceSync = true;
        this.threadSafetyLevel = ThreadSafetyLevel.SYNC_ONLY;
        this.damage = PlaceholderDouble.of(mlc.getString(new String[]{"damage", "amount", "a", "d"}, "0"));
        this.damagePlayer = PlaceholderBoolean.of(mlc.getString(new String[]{"damageplayer", "dp"}, "false"));
        this.ignoreInvulnerable = PlaceholderBoolean.of(mlc.getString(new String[]{"ignoreinvulnerable", "ii"}, "false"));
        this.sound = PlaceholderString.of(mlc.getString(new String[]{"sound", "s"}, "item.trident.hit"));
        this.disabledServer = PlaceholderString.of(mlc.getString(new String[]{"disabledServer", "ds"}, "家"));
    }

    public static SkillResult applyTrueDamage(LivingEntity l, LivingEntity c, double damage, boolean damagePlayer, boolean ignoreInvulnerable) {
        return applyTrueDamage(l, c, damage, damagePlayer, ignoreInvulnerable, "item.trident.hit");
    }
    /**
     * Apply true damage to a target entity from a caster.
     *
     * @param l                  target living entity
     * @param c                  caster living entity
     * @param damage             damage amount
     * @param damagePlayer       whether players may be damaged
     * @param ignoreInvulnerable whether to ignore invulnerability
     * @param sound              sound to play on hit
     * @return SkillResult indicating the result of the operation
     */
    @SuppressWarnings("removal")
    public static SkillResult applyTrueDamage(LivingEntity l, LivingEntity c, double damage, boolean damagePlayer, boolean ignoreInvulnerable, String sound) {
        if (!damagePlayer && l instanceof Player) return SkillResult.INVALID_TARGET;
        if (!l.isValid() || (l.isInvulnerable() && !ignoreInvulnerable)) return SkillResult.INVALID_TARGET;
        if (damage <= 0) return SkillResult.INVALID_CONFIG;

        var trueEvent = new EntityTrueDamageEvent(l, c, damage);
        Bukkit.getPluginManager().callEvent(trueEvent);
        if (trueEvent.isCancelled()) return SkillResult.INVALID_TARGET;
        damage = trueEvent.getDamage();
        if (damage <= 0) return SkillResult.CONDITION_FAILED;
        var source = DamageSource.builder(c instanceof Player ? DamageType.PLAYER_ATTACK : DamageType.MOB_ATTACK).withDirectEntity(c).withCausingEntity(c).build();
        CraftLivingEntity cL = (CraftLivingEntity) l;
        var health = (float) Math.clamp(cL.getHealth() - damage, 0, cL.getMaxHealth());
        if (cL.getHandle().generation && health == 0) {
            cL.getHandle().discard(null);
            return SkillResult.INVALID_TARGET;
        }
        l.playHurtAnimation(0);
        l.getWorld().playSound(l, sound, 1, 1);

        var e = new EntityDamageByEntityEvent(c, l, EntityDamageEvent.DamageCause.ENTITY_ATTACK, source, damage);
        if (health == 0) {
            l.setLastDamageCause(e);
            cL.getHandle().die(((CraftDamageSource) source).getHandle());
        }
        cL.getHandle().setHealth(health);
        if (!(l instanceof Player)) {
            l.getScheduler().runAtFixedRate(MythicMobsAddon.plugin, task -> {
                if (!l.isValid()) {
                    l.setHealth(0);
                    task.cancel();
                }
            }, null, 5, 1);
        }

        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata skillMetadata, AbstractEntity abstractEntity) {
        PlaceholderContext context = PlaceholderContext.builder().meta(skillMetadata).entity(abstractEntity).build();
        if (MythicMobsAddon.plugin.getServer().getName().contains(this.disabledServer.get(context))) return SkillResult.CONDITION_FAILED;
        if (!(abstractEntity.getBukkitEntity() instanceof LivingEntity l) || !(skillMetadata.getCaster().getEntity().getBukkitEntity() instanceof LivingEntity c)) {
            return SkillResult.INVALID_TARGET;
        }
        double dmg = this.damage.get(context);
        boolean dp = this.damagePlayer.get(context);
        boolean ii = this.ignoreInvulnerable.get(context);
        String sound = this.sound.get(context);

        return applyTrueDamage(l, c, dmg, dp, ii, sound);
    }
}
