package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PolymorphicPlaceholder;
import io.lumine.mythic.core.logging.MythicLogger;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.variables.*;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.Locale;

public class ReadPDCMechanic extends VariableMechanic implements ITargetedEntitySkill {
    private final String slot;
    private final NamespacedKey namespacedKey;
    protected VariableType type;

    public ReadPDCMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);
        this.slot = mlc.getString(new String[] { "slot" , "s"}, "HAND");
        String nk = mlc.getString(new String[] { "namespacedKey" , "n"}, "mmaddon:pdc");
        String strType = mlc.getString(new String[]{"type", "t"}, VariableType.STRING.toString());
        try {
            this.type = VariableType.valueOf(strType.toUpperCase());
        } catch (IllegalArgumentException e) {
            MythicLogger.errorMechanicConfig(this, mlc, "'" + strType + "' is not a valid variable type.");
        }
        this.namespacedKey = NamespacedKey.fromString(nk);
        if(namespacedKey == null){
            Locale locale = Locale.getDefault();
            if(locale.equals(Locale.CHINA)){
                MythicLogger.errorMechanicConfig(this, mlc,"readPDC技能中的namespacedKey参数格式有误");
            }else{
                MythicLogger.errorMechanicConfig(this, mlc,"Format of namespacedKey parameter in savePDC mechanic is wrong");
            }
        }
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata skillMetadata, AbstractEntity abstractEntity) {
        Entity entity = abstractEntity.getBukkitEntity();
        if(!(entity instanceof LivingEntity target)){
            return SkillResult.INVALID_TARGET;
        }
        EquipmentSlot equipmentSlot = null;
        int numberSlot = -1;
        try{
            equipmentSlot = EquipmentSlot.valueOf(this.slot.toUpperCase(Locale.ROOT));
        }catch(IllegalArgumentException e){
            if(!(entity instanceof Player)){
                return SkillResult.INVALID_TARGET;
            }
            try{
                numberSlot = Integer.parseInt(this.slot);
                if(numberSlot < 0 || numberSlot > 40){
                    return SkillResult.INVALID_CONFIG;
                }
            }catch(NumberFormatException ex){
                return SkillResult.INVALID_CONFIG;
            }
        }
        ItemStack itemStack;
        if(equipmentSlot != null){
            EntityEquipment entityEquipment = target.getEquipment();
            if(entityEquipment == null || !target.canUseEquipmentSlot(equipmentSlot)){
                return SkillResult.INVALID_TARGET;
            }
            itemStack = entityEquipment.getItem(equipmentSlot);
        } else {
            Player player = (Player) target;
            itemStack = player.getInventory().getItem(numberSlot);
        }
        if(itemStack == null || itemStack.isEmpty()){
            return SkillResult.INVALID_TARGET;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return SkillResult.INVALID_TARGET;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String value = null;

        if (pdc.has(namespacedKey, PersistentDataType.STRING)) {
            value = pdc.get(namespacedKey, PersistentDataType.STRING);
        } else if (pdc.has(namespacedKey, PersistentDataType.INTEGER)) {
            value = String.valueOf(pdc.get(namespacedKey, PersistentDataType.INTEGER));
        } else if (pdc.has(namespacedKey, PersistentDataType.DOUBLE)) {
            value = String.valueOf(pdc.get(namespacedKey, PersistentDataType.DOUBLE));
        } else if (pdc.has(namespacedKey, PersistentDataType.LONG)) {
            value = String.valueOf(pdc.get(namespacedKey, PersistentDataType.LONG));
        } else if (pdc.has(namespacedKey, PersistentDataType.FLOAT)) {
            value = String.valueOf(pdc.get(namespacedKey, PersistentDataType.FLOAT));
        }

        if (value == null) {
            return SkillResult.CONDITION_FAILED;
        }

        VariableRegistry registry = this.getVariableManager().getRegistry(this.scope, skillMetadata, abstractEntity);
        if (registry == null) {
            MythicLogger.errorMechanicConfig(this, this.config, "Failed to get variable registry");
            return SkillResult.INVALID_CONFIG;
        }

        VariableInfo<?> info = VariableUtils.variableInfoByType.get(this.type);
        if (info == null || info.polymorphicConstructor() == null) {
            MythicLogger.errorMechanicConfig(this, this.config, "Failed to get variable type constructor");
            return SkillResult.INVALID_CONFIG;
        }

        long expireTime = this.duration > 1L ? System.currentTimeMillis() + this.duration : this.duration;
        PolymorphicPlaceholder placeholder = PolymorphicPlaceholder.of(value);
        PlaceholderContext context = PlaceholderContext.of(skillMetadata, abstractEntity, null, null, null);
        Variable var = info.polymorphicConstructor().create(placeholder, context, expireTime);

        registry.put(this.key.get(skillMetadata, abstractEntity), var);

        return SkillResult.SUCCESS;
    }
}
