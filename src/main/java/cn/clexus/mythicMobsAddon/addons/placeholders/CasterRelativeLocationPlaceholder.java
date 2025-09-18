package cn.clexus.mythicMobsAddon.addons.placeholders;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.skills.placeholders.PlaceholderMeta;
import io.lumine.mythic.core.skills.placeholders.types.MetaPlaceholder;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class CasterRelativeLocationPlaceholder implements MetaPlaceholder {
    @Override
    public String apply(PlaceholderMeta placeholderMeta, String s) {
        Location casterLoc = BukkitAdapter.adapt(placeholderMeta.getCaster().getLocation());
        int idx = s.indexOf('.');
        String axis = s.substring(0, idx);
        String[] values = s.substring(idx + 1).split(",");
        if (values.length != 3) {
            return "0";
        }
        double rx, ry, rz;
        rx = Double.parseDouble(values[0]);
        ry = Double.parseDouble(values[1]);
        rz = Double.parseDouble(values[2]);
        switch (axis) {
            case "ox" -> {
                return r(casterLoc, rx, ry, rz).getX() - casterLoc.getX() + "";
            }
            case "oy" -> {
                return r(casterLoc, rx, ry, rz).getY() - casterLoc.getY() + "";
            }
            case "oz" -> {
                return r(casterLoc, rx, ry, rz).getZ() - casterLoc.getZ() + "";
            }
            case "ax" -> {
                return r(casterLoc, rx, ry, rz).getX() + "";
            }
            case "ay" -> {
                return r(casterLoc, rx, ry, rz).getY() + "";
            }
            case "az" -> {
                return r(casterLoc, rx, ry, rz).getZ() + "";
            }
        }
        return "0";
    }
    private static Location r(Location origin, double rx, double ry, double rz) {
        Vector forward = origin.getDirection().normalize();
        Vector upWorld = new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(upWorld).normalize();
        if (right.lengthSquared() < 0.0001) {
            right = new Vector(1, 0, 0);
        }
        Vector up = right.clone().crossProduct(forward).normalize();

        Vector offset = right.multiply(rx)
                .add(up.multiply(ry))
                .add(forward.multiply(rz));

        return origin.clone().add(offset);
    }
}
