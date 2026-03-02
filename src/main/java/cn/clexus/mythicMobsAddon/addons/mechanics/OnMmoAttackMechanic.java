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
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.auras.Aura;
import io.lumine.mythic.lib.api.event.AttackEvent;
import io.lumine.mythic.lib.api.event.PlayerAttackEvent;
import io.lumine.mythic.lib.damage.DamageMetadata;
import io.lumine.mythic.lib.damage.DamageType;
import org.bukkit.event.EventPriority;

import java.io.File;
import java.util.Optional;

public class OnMmoAttackMechanic extends Aura implements ITargetedEntitySkill {
    protected Optional<Skill> onAttackSkill = Optional.empty();
    protected String onAttackSkillName;
    protected boolean cancelDamage;
    protected boolean modDamage = false;
    protected PlaceholderString modDamageType;
    protected PlaceholderDouble damageAdd;
    protected PlaceholderDouble damageMult;

    public OnMmoAttackMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
        super(manager, file, skill, mlc);
        this.onAttackSkillName = mlc.getString(new String[]{"onattackskill", "onattack", "oa", "onmelee", "onhitskill", "onhit", "oh"});
        this.modDamageType = mlc.getPlaceholderString(new String[]{"modDamageType", "damagetype"}, null);
        this.cancelDamage = mlc.getBoolean(new String[]{"cancelevent", "ce", "canceldamage", "cd"}, false);

        String dAdd = mlc.getString(new String[]{"damageadd", "add", "a"}, "0");
        String dMult = mlc.getString(new String[]{"damagemultiplier", "multiplier", "m"}, "1");
        if (dAdd != null || dMult != null) {
            this.modDamage = true;
        }

        this.damageAdd = PlaceholderDouble.of(dAdd);
        this.damageMult = PlaceholderDouble.of(dMult);

        this.getManager().queueSecondPass(() -> {
            if (this.onAttackSkillName != null) {
                this.onAttackSkill = this.getManager().getSkill(file, this, this.onAttackSkillName);
            }
        });
    }

    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        new OnMmoAttackMechanic.Tracker(data, target);
        return SkillResult.SUCCESS;
    }

    private class Tracker extends Aura.AuraTracker implements Runnable {
        public Tracker(SkillMetadata data, AbstractEntity entity) {
            super(data.getCaster(), entity, data);
            this.start();
        }

        public void auraStart() {
            // 关键修改：订阅 AttackEvent 而非 EntityDamageByEntityEvent
            this.registerAuraComponent(Events.subscribe(AttackEvent.class, EventPriority.LOW)
                    .filter((event) -> !event.isCancelled())
                    .filter((event) -> event.getAttack().hasAttacker() && event.getAttack().getAttacker().getEntity().getUniqueId().equals(this.entity.get().getUniqueId()))
                    .handler(this::exeEvent));
            this.registerAuraComponent(Events.subscribe(PlayerAttackEvent.class, EventPriority.LOW)
                    .filter((event) -> !event.isCancelled())
                    .filter((event) -> event.getAttack().hasAttacker() && event.getAttack().getAttacker().getEntity().getUniqueId().equals(this.entity.get().getUniqueId()))
                    .handler(this::exeEvent));

            this.executeAuraSkill(OnMmoAttackMechanic.this.onStartSkill, this.skillMetadata);
        }

        private void exeEvent(AttackEvent event){
            AbstractEntity target = BukkitAdapter.adapt(event.getEntity());
            SkillMetadata meta = this.skillMetadata;
            meta.setTrigger(target);

            // 执行技能
            if (this.executeTargetedAuraSkill(OnMmoAttackMechanic.this.onAttackSkill, meta, target)) {
                this.consumeCharge();

                // 处理伤害取消
                if (OnMmoAttackMechanic.this.cancelDamage) {
                    event.setCancelled(true);
                    return;
                }
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

                // 处理伤害修改
                if (OnMmoAttackMechanic.this.modDamage) {
                    double add = OnMmoAttackMechanic.this.damageAdd.get(meta, target);
                    double mult = OnMmoAttackMechanic.this.damageMult.get(meta, target);

                    String typeStr = OnMmoAttackMechanic.this.modDamageType == null ? null : OnMmoAttackMechanic.this.modDamageType.get(meta, target);

                    // 获取事件中的 DamageMetadata 对象
                    DamageMetadata damageMeta = event.getAttack().getDamage();

                    if (typeStr == null || typeStr.isEmpty()) {
                        // 1. 全局增加伤害：通过 add 方法添加一个新的 DamagePacket
                        if (add != 0) {
                            damageMeta.add(add);
                        }
                        // 2. 全局乘法修正：调用 multiplicativeModifier 遍历所有 packet 进行缩放
                        if (mult != 1) {
                            damageMeta.multiplicativeModifier(mult);
                        }
                    } else {
                        try {
                            DamageType dType = DamageType.valueOf(typeStr.toUpperCase());

                            // 3. 特定类型加法：添加一个带有指定 DamageType 的新 Packet
                            if (add != 0) {
                                damageMeta.add(add, dType);
                            }

                            // 4. 特定类型乘法：仅对含有该 DamageType 的 Packet 进行乘法修正
                            if (mult != 1) {
                                damageMeta.multiplicativeModifier(mult, dType);
                            }
                        } catch (IllegalArgumentException e) {
                            // 如果解析 DamageType 失败，回退到全局修改逻辑
                            if (add != 0) damageMeta.add(add);
                            if (mult != 1) damageMeta.multiplicativeModifier(mult);
                        }
                    }
                }
            }
        }
    }
}