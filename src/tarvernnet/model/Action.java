package tarvernnet.model;

import java.util.ArrayList;

public class Action {
    enum Type {
        MELEE,
        RANGED,
        MAGIC
    }
    private String name;
    private Type type;
    private int range;
    private int toHit;
    private ArrayList<Integer> damageDice;
    private String damageType;

    public Action(String name, Type type, int range, int toHit, ArrayList<Integer> damageDice, String damageType) {
        this.name = name;
        this.type = type;
        this.range = range;
        this.toHit = toHit;
        this.damageDice = damageDice;
        this.damageType = damageType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getToHit() {
        return toHit;
    }

    public void setToHit(int toHit) {
        this.toHit = toHit;
    }

    public ArrayList<Integer> getDamageDice() {
        return damageDice;
    }

    public void setDamageDice(ArrayList<Integer> damageDice) {
        this.damageDice = damageDice;
    }

    public String getDamageType() {
        return damageType;
    }

    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }
}
