package cn.clexus.mythicMobsAddon.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Locale;

public class PDCUtil {
    public static String getPDCData(Entity entity, String s) {
        //slot$nk
        String[] parts =  s.split("\\$");
        String slot = parts[0];
        String nk = parts[1];
        NamespacedKey namespacedKey = NamespacedKey.fromString(nk);
        if(namespacedKey == null){
            return "";
        }
        if(!(entity instanceof LivingEntity target)){
            return "";
        }
        EquipmentSlot equipmentSlot = null;
        int numberSlot = -1;
        try{
            equipmentSlot = EquipmentSlot.valueOf(slot.toUpperCase(Locale.ROOT));
        }catch(IllegalArgumentException e){
            if(!(entity instanceof Player)){
                return "";
            }
            try{
                numberSlot = Integer.parseInt(slot);
                if(numberSlot < 0 || numberSlot > 40){
                    return "";
                }
            }catch(NumberFormatException ex){
                return "";
            }
        }
        ItemStack itemStack;
        if(equipmentSlot != null){
            EntityEquipment entityEquipment = target.getEquipment();
            if(entityEquipment == null || !target.canUseEquipmentSlot(equipmentSlot)){
                return "";
            }
            itemStack = entityEquipment.getItem(equipmentSlot);
        } else {
            Player player = (Player) target;
            itemStack = player.getInventory().getItem(numberSlot);
        }
        if(itemStack == null || itemStack.isEmpty()){
            return "";
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return "";
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String value = null;

        if(pdc.has(namespacedKey)){
            value = ((CraftPersistentDataContainer)pdc).getTag(nk).toString();
        }

        if (value == null) {
            return "";
        }
        if(value.startsWith("'\"") && value.endsWith("\"'")){
            value = value.substring(2, value.length() - 2);
        }
        return value;
    }
}
