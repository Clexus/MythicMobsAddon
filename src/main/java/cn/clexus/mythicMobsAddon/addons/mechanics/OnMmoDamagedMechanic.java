package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.utils.Events;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.auras.Aura;
import io.lumine.mythic.lib.api.event.AttackEvent;
import io.lumine.mythic.lib.api.event.PlayerAttackEvent;
import io.lumine.mythic.lib.damage.DamageMetadata;
import io.lumine.mythic.lib.damage.DamageType;
import org.bukkit.event.EventPriority;

import java.io.File;
import java.util.Optional;

public class OnMmoDamagedMechanic extends Aura implements ITargetedEntitySkill {
    protected Optional<Skill> onDamagedSkill = Optional.empty();
    protected String onDamagedSkillName;
    protected boolean cancelDamage;
    protected boolean modDamage = false;
    protected PlaceholderString modDamageType;
    protected PlaceholderDouble damageSub; // 注意：受击通常是减免(Sub)，攻击通常是增加(Add)
    protected PlaceholderDouble damageMult;
    protected PlaceholderDouble damageSet;


    public OnMmoDamagedMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
        super(manager, file, skill, mlc);
        this.onDamagedSkillName = mlc.getString(new String[]{"ondamagedskill", "ondamaged", "od", "onhitskill", "onhit", "oh"});
        this.modDamageType = mlc.getPlaceholderString(new String[]{"modDamageType", "damagetype"}, null);
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

    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        new OnMmoDamagedMechanic.Tracker(data, target);
        return SkillResult.SUCCESS;
    }

    private class Tracker extends Aura.AuraTracker implements Runnable {
        public Tracker(SkillMetadata data, AbstractEntity entity) {
            super(entity, data);
            this.start();
        }

        public void auraStart() {
            // 订阅 MythicLib 的 AttackEvent
            this.registerAuraComponent(Events.subscribe(AttackEvent.class, EventPriority.HIGHEST)
                    .filter((event) -> !event.isCancelled())
                    // 【关键修改点】：过滤受击者，确保是 Aura 的拥有者在挨打
                    .filter((event) -> event.getAttack().hasAttacker() && skillMetadata.getEntityTargets().stream().anyMatch(e -> e.getUniqueId().equals(event.getEntity().getUniqueId())))
                    .handler(this::exeEvent));
            this.registerAuraComponent(Events.subscribe(PlayerAttackEvent.class, EventPriority.HIGHEST)
                    .filter((event) -> !event.isCancelled())
                    // 【关键修改点】：过滤受击者，确保是 Aura 的拥有者在挨打
                    .filter((event) -> event.getAttack().hasAttacker() && skillMetadata.getEntityTargets().stream().anyMatch(e -> e.getUniqueId().equals(event.getEntity().getUniqueId())))
                    .handler(this::exeEvent));

            this.executeAuraSkill(OnMmoDamagedMechanic.this.onStartSkill, this.skillMetadata);
        }

        private void exeEvent(AttackEvent event) {
            AbstractEntity attacker = BukkitAdapter.adapt(event.getAttack().getAttacker().getEntity());
            SkillMetadata meta = this.skillMetadata.deepClone();
            meta.setTrigger(attacker);

            double dmg = 0;
            for (var p : event.getDamage().getPackets()) {
                dmg += p.getFinalValue();
            }
            dmg = Math.max(0, dmg);
            meta.getVariables().putDouble("damage-amount", dmg);
            var nameList = event.getDamage().collectTypes().stream().map(DamageType::name).toList();
            String types = String.join(",", nameList);
            meta.getVariables().putString("damage-types", types);
            meta.getVariables().putString("damage-cause", event.toBukkit().getCause().toString());
            if (this.executeAuraSkill(OnMmoDamagedMechanic.this.onDamagedSkill, meta)) {
                this.consumeCharge();
            }

            // 执行被击中时触发的技能

            if (OnMmoDamagedMechanic.this.cancelDamage) {
                event.setCancelled(true);
                return;
            }

            // 处理伤害修改
            if (OnMmoDamagedMechanic.this.modDamage) {
                // 注意：sub 为正数时代表减伤，所以加法逻辑里要传负值
                PlaceholderContext context = PlaceholderContext.builder().meta(meta).entity(attacker).build();
                double sub = OnMmoDamagedMechanic.this.damageSub.get(context);
                double mult = OnMmoDamagedMechanic.this.damageMult.get(context);
                double set = OnMmoDamagedMechanic.this.damageSet.get(context);

                String typeStr = OnMmoDamagedMechanic.this.modDamageType == null ? null : OnMmoDamagedMechanic.this.modDamageType.get(context);
                DamageMetadata damageMeta = event.getAttack().getDamage();

                if (typeStr == null || typeStr.isEmpty()) {
                    if (set != -1) {
                        damageMeta.multiplicativeModifier(set / damageMeta.getDamage());
                    } else {
                        // 全局修改
                        if (sub != 0) damageMeta.add(-sub);
                        if (mult != 1) damageMeta.multiplicativeModifier(mult);
                    }
                } else {
                    try {
                        DamageType dType = DamageType.valueOf(typeStr.toUpperCase());
                        // 特定类型修改
                        if (set != -1) {
                            damageMeta.multiplicativeModifier(set / damageMeta.getDamage(dType), dType);
                        } else {
                            if (sub != 0) damageMeta.add(-sub, dType);
                            if (mult != 1) damageMeta.multiplicativeModifier(mult, dType);
                        }
                    } catch (IllegalArgumentException e) {
                        // 回退
                        if (set != -1) {
                            damageMeta.multiplicativeModifier(0);
                            damageMeta.add(set);
                        } else {
                            // 全局修改
                            if (sub != 0) damageMeta.add(-sub);
                            if (mult != 1) damageMeta.multiplicativeModifier(mult);
                        }
                    }
                }
            }

        }
    }
}