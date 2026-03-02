package cn.clexus.mythicMobsAddon.addons.mechanics;

import cn.clexus.mythicMobsAddon.events.EntityTrueDamageEvent;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.utils.Events;
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

    public OnTrueDamagedMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
        super(manager, file, skill, mlc);
        this.onDamagedSkillName = mlc.getString(new String[]{"ondamagedskill", "ondamaged", "od", "onhitskill", "onhit", "oh"});
        this.cancelDamage = mlc.getBoolean(new String[]{"cancelevent", "ce", "canceldamage", "cd"}, false);

        String dSub = mlc.getString(new String[]{"damagesub", "sub", "s"}, "0");
        String dMult = mlc.getString(new String[]{"damagemultiplier", "multiplier", "m"}, "1");

        if (!dSub.equals("0") || !dMult.equals("1")) {
            this.modDamage = true;
        }

        this.damageSub = PlaceholderDouble.of(dSub);
        this.damageMult = PlaceholderDouble.of(dMult);

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

    private class Tracker extends Aura.AuraTracker {
        public Tracker(SkillMetadata data, AbstractEntity entity) {
            super(data.getCaster(), entity, data);
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

            // MONITOR 优先级用于执行技能和变量注册
            this.registerAuraComponent(Events.subscribe(EntityTrueDamageEvent.class, EventPriority.MONITOR)
                    .filter(event -> !event.isCancelled())
                    .filter(event -> this.entity.isPresent() && event.getEntity().getUniqueId().equals(this.entity.get().getUniqueId()))
                    .handler(this::handleSkillTrigger));

            this.executeAuraSkill(OnTrueDamagedMechanic.this.onStartSkill, this.skillMetadata);
        }

        private void handleDamageModification(EntityTrueDamageEvent event) {
            if (OnTrueDamagedMechanic.this.cancelDamage) {
                event.setCancelled(true);
                return;
            }

            if (OnTrueDamagedMechanic.this.modDamage) {
                AbstractEntity attacker = BukkitAdapter.adapt(event.getDamager());
                SkillMetadata meta = this.skillMetadata.deepClone();
                meta.setTrigger(attacker);

                double currentDmg = event.getDamage();
                double sub = OnTrueDamagedMechanic.this.damageSub.get(meta, attacker);
                double mult = OnTrueDamagedMechanic.this.damageMult.get(meta, attacker);

                // 计算新伤害值: (当前伤害 - 减值) * 倍率
                double newDmg = (currentDmg - sub) * mult;
                event.setDamage(Math.max(0, newDmg));
            }
        }

        private void handleSkillTrigger(EntityTrueDamageEvent event) {
            var meta = this.skillMetadata.deepClone();
            AbstractEntity attacker = BukkitAdapter.adapt(event.getDamager());

            // 设置触发者为攻击者
            meta.setTrigger(attacker);

            // 注册变量供 MM 技能使用
            meta.getVariables().putDouble("damage-amount", event.getDamage());

            // 执行子技能
            if (OnTrueDamagedMechanic.this.onDamagedSkill.isPresent()) {
                if (this.executeAuraSkill(OnTrueDamagedMechanic.this.onDamagedSkill, meta)) {
                    this.consumeCharge();
                }
            }
        }
    }
}