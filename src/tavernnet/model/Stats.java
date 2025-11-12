package tavernnet.model;

import java.util.ArrayList;
import java.util.HashMap;

public class Stats {
    private HashMap<String, Integer> main;
    private HashMap<String, Integer> modifiers;
    private HashMap<String, Integer> passive;
    private HashMap<String, Integer> combat;

    public Stats(){}

    public Stats(HashMap<String, Integer> main, HashMap<String,
                Integer> modifiers, HashMap<String, Integer> passive,
                 HashMap<String, Integer> combat) {
        this.main = main;
        this.modifiers = modifiers;
        this.passive = passive;
        this.combat = combat;
    }

    public HashMap<String, Integer> getMain() {
        return main;
    }

    public void setMain(HashMap<String, Integer> main) {
        this.main = main;
    }

    public HashMap<String, Integer> getModifiers() {
        return modifiers;
    }

    public void setModifiers(HashMap<String, Integer> modifiers) {
        this.modifiers = modifiers;
    }

    public HashMap<String, Integer> getPassive() {
        return passive;
    }

    public void setPassive(HashMap<String, Integer> passive) {
        this.passive = passive;
    }

    public HashMap<String, Integer> getCombat() {
        return combat;
    }

    public void setCombat(HashMap<String, Integer> combat) {
        this.combat = combat;
    }

}
