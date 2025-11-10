package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.core.logging.MythicLogger;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.Locale;

public class SavePDCMechanic extends SkillMechanic implements ITargetedEntitySkill {
    private final String slot;
    private final NamespacedKey namespacedKey;
    private final PlaceholderString data;

    public SavePDCMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);
        this.slot = mlc.getString(new String[] { "slot" , "s"}, "HAND");
        this.data = PlaceholderString.of(mlc.getString(new String[] { "data" , "d"}, "DATA"));
        String nk = mlc.getString(new String[] { "namespacedKey" , "n"}, "mmaddon:pdc");
        this.namespacedKey = NamespacedKey.fromString(nk);
        if(namespacedKey == null){
            Locale locale = Locale.getDefault();
            if(locale.equals(Locale.CHINA)){
                MythicLogger.errorMechanicConfig(this, mlc,"savePDC技能中的namespacedKey参数格式有误");
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
        itemStack.editMeta(meta -> meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, this.data.get(skillMetadata, abstractEntity)));
        return SkillResult.SUCCESS;
    }
}
