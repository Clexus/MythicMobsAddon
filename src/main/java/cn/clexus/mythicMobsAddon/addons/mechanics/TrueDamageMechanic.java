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
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.damage.CraftDamageSource;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

public class TrueDamageMechanic extends SkillMechanic implements ITargetedEntitySkill {
    private final PlaceholderDouble damage;
    private final PlaceholderBoolean damagePlayer;
    private final PlaceholderBoolean ignoreInvulnerable;
    private final Component text = MiniMessage.miniMessage().deserialize("<dark_grey>☠ ");

    public TrueDamageMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);
        this.forceSync = true;
        this.threadSafetyLevel = ThreadSafetyLevel.SYNC_ONLY;
        this.damage = PlaceholderDouble.of(mlc.getString(new String[]{"damage", "amount", "a", "d"}, "0"));
        this.damagePlayer = PlaceholderBoolean.of(mlc.getString(new String[]{"damageplayer", "dp"}, "false"));
        this.ignoreInvulnerable = PlaceholderBoolean.of(mlc.getString(new String[]{"ignoreinvulnerable", "ii"}, "false"));

    }

    @Override
    public SkillResult castAtEntity(SkillMetadata skillMetadata, AbstractEntity abstractEntity) {
        if (MythicMobsAddon.plugin.getServer().getName().equals("家")) return SkillResult.CONDITION_FAILED;
        if (!(abstractEntity.getBukkitEntity() instanceof LivingEntity l) || !(skillMetadata.getCaster().getEntity().getBukkitEntity() instanceof LivingEntity c) || (!damagePlayer.get(skillMetadata) && l instanceof Player))
            return SkillResult.INVALID_TARGET;
        boolean ignoreInvulnerable = this.ignoreInvulnerable.get(skillMetadata);
        if (l.isInvulnerable() && !ignoreInvulnerable) return SkillResult.INVALID_TARGET;
        double dmg = this.damage.get(skillMetadata, abstractEntity);
        if (dmg <= 0) return SkillResult.INVALID_CONFIG;
        var trueEvent = new EntityTrueDamageEvent(l, c, dmg);
        Bukkit.getPluginManager().callEvent(trueEvent);
        if (trueEvent.isCancelled()) return SkillResult.INVALID_TARGET;
        double damage = trueEvent.getDamage();
        if (damage <= 0) return SkillResult.CONDITION_FAILED;
        var source = DamageSource.builder(c instanceof Player ? DamageType.PLAYER_ATTACK : DamageType.MOB_ATTACK).withDirectEntity(c).withCausingEntity(c).build();
        CraftLivingEntity cL = (CraftLivingEntity) l;
        var health = (float) Math.clamp(cL.getHealth() - damage, 0, cL.getMaxHealth());
        if (cL.getHandle().generation && health == 0) {
            cL.getHandle().discard(null);
            return SkillResult.INVALID_TARGET;
        }
        l.playHurtAnimation(0);
        l.getWorld().playSound(l, Sound.ITEM_TRIDENT_HIT, 1, 1);
        var di = l.getWorld().spawn(l.getEyeLocation(), TextDisplay.class, d -> {
            d.setTeleportDuration(2);
            d.setInterpolationDelay(2);
            d.setBillboard(Display.Billboard.CENTER);
            d.text(text.append(Component.text(BigDecimal.valueOf(damage).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()).color(NamedTextColor.DARK_RED)));
            d.setPersistent(false);
        });
        var v = new Vector(0, 0, 0);
        while (v.length() == 0) {
            var x = ThreadLocalRandom.current().nextDouble(-1, 1);
            var y = ThreadLocalRandom.current().nextDouble(0, 1);
            var z = ThreadLocalRandom.current().nextDouble(-1, 1);
            v = new Vector(x, y, z);
        }
        v.normalize();
        var target = l.getEyeLocation().add(v);
        di.teleport(target);
        Bukkit.getScheduler().runTaskLater(MythicMobsAddon.plugin, di::remove, 20);

        var e = new EntityDamageByEntityEvent(c, l, EntityDamageEvent.DamageCause.ENTITY_ATTACK, source, damage);
        if (health == 0) {
            l.setLastDamageCause(e);
            cL.getHandle().die(((CraftDamageSource) source).getHandle());
        }
        cL.getHandle().setHealth(health);
        l.getScheduler().runAtFixedRate(MythicMobsAddon.plugin, task -> {
            if (!l.isValid()) l.setHealth(0);
        }, null, 1, 1);


        return SkillResult.SUCCESS;
    }
}
