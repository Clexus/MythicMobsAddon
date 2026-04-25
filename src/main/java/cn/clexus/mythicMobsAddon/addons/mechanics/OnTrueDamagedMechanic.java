package cn.clexus.mythicMobsAddon.addons.mechanics;

import cn.clexus.mythicMobsAddon.events.EntityTrueDamageEvent;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.*;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.utils.Events;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.auras.Aura;
import org.bukkit.event.EventPriority;

import java.io.File;
import java.util.Optional;

public class OnTrueDamagedMechanic extends Aura implements ITargetedEntitySkill {
    protected Optional<Skill> onDamagedSkill = Optional.empty();
    protected String onDamagedSkillName;
    protected boolean cancelDamage;
    protected boolean modDamage = false;
    protected PlaceholderDouble damageSub;
    protected PlaceholderDouble damageMult;
    protected PlaceholderDouble damageSet;

    public OnTrueDamagedMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
        super(manager, file, skill, mlc);
        this.onDamagedSkillName = mlc.getString(new String[]{"ondamagedskill", "ondamaged", "od", "onhitskill", "onhit", "oh"});
        this.cancelDamage = mlc.getBoolean(new String[]{"cancelevent", "ce", "canceldamage", "cd"}, false);

        String dSub = mlc.getString(new String[]{"damagesub", "sub", "s"}, "0");
        String dMult = mlc.getString(new String[]{"damagemultiplier", "multiplier", "m"}, "1");
        String dSet = mlc.getString(new String[]{"damageset", "set"}, "-1");

        if (!dSub.equals("0") || !dMult.equals("1") || !dSet.equals("-1")) {
            this.modDamage = true;
        }

        this.damageSub = PlaceholderDouble.of(dSub);
        this.damageMult = PlaceholderDouble.of(dMult);
        this.damageSet = PlaceholderDouble.of(dSet);

        this.getManager().queueSecondPass(() -> {
            if (this.onDamagedSkillName != null) {
                this.onDamagedSkill = this.getManager().getSkill(file, this, this.onDamagedSkillName);
            }
        });
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        new Tracker(data, target);
        return SkillResult.SUCCESS;
    }

    private class Tracker extends Aura.AuraTracker implements IParentSkill, Runnable {
        public Tracker(SkillMetadata data, AbstractEntity entity) {
            super(entity, data);
            this.start();
        }

        @Override
        public void auraStart() {
            // 订阅自定义的 EntityTrueDamageEvent
            // HIGHEST 优先级用于修改伤害数值
            this.registerAuraComponent(Events.subscribe(EntityTrueDamageEvent.class, EventPriority.HIGHEST)
                    .filter(event -> !event.isCancelled())
                    // 过滤：确保受击者是带有此 Aura 的实体
                    .filter(event -> this.entity.isPresent() && event.getEntity().getUniqueId().equals(this.entity.get().getUniqueId()))
                    .handler(this::handleDamageModification));

            this.executeAuraSkill(OnTrueDamagedMechanic.this.onStartSkill, this.skillMetadata);
        }

        private void handleDamageModification(EntityTrueDamageEvent event) {
            var meta = this.skillMetadata.deepClone();
            AbstractEntity attacker = BukkitAdapter.adapt(event.getDamager());

            meta.setTrigger(attacker);

            meta.getVariables().putDouble("damage-amount", event.getDamage());

            // 执行子技能
            if (OnTrueDamagedMechanic.this.onDamagedSkill.isPresent()) {
                if (this.executeAuraSkill(OnTrueDamagedMechanic.this.onDamagedSkill, meta)) {
                    this.consumeCharge();
                }
            }

            if (OnTrueDamagedMechanic.this.cancelDamage) {
                event.setCancelled(true);
                return;
            }

            if (OnTrueDamagedMechanic.this.modDamage) {
                PlaceholderContext context = PlaceholderContext.builder().meta(meta).entity(attacker).build();

                double currentDmg = event.getDamage();
                double sub = OnTrueDamagedMechanic.this.damageSub.get(context);
                double mult = OnTrueDamagedMechanic.this.damageMult.get(context);
                double set = OnTrueDamagedMechanic.this.damageSet.get(context);

                double newDmg = set != -1 ? set : (currentDmg - sub) * mult;
                event.setDamage(Math.max(0, newDmg));
            }
        }

    }
}