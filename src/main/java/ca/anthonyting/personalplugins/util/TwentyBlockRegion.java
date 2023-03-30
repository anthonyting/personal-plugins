package ca.anthonyting.personalplugins.util;

import org.bukkit.Location;

public class TwentyBlockRegion {
    private final int x;
    private final int z;

    public TwentyBlockRegion(Location location) {
        this.x = (int) Math.round(location.getX() / 20);
        this.z = (int) Math.round(location.getZ() / 20);
    }

    @Override
    public int hashCode() {
        return x * 31 + z;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TwentyBlockRegion that = (TwentyBlockRegion) obj;
        return x == that.x && z == that.z;
    }

    @Override
    public String toString() {
        return "TenBlockRegion{" +
                "x=" + x +
                ", z=" + z +
                '}';
    }
}
