package tavernnet.model;

import java.util.Map;

public class Stats {
    private Map<String, Integer> main;
    private Map<String, Integer> modifiers;
    private Map<String, Integer> passive;
    private Map<String, Integer> combat;

    public Stats(){}

    public Stats(Map<String, Integer> main, Map<String,
                Integer> modifiers, Map<String, Integer> passive,
                 Map<String, Integer> combat) {
        this.main = main;
        this.modifiers = modifiers;
        this.passive = passive;
        this.combat = combat;
    }

    public Map<String, Integer> getMain() {
        return main;
    }

    public void setMain(Map<String, Integer> main) {
        this.main = main;
    }

    public Map<String, Integer> getModifiers() {
        return modifiers;
    }

    public void setModifiers(Map<String, Integer> modifiers) {
        this.modifiers = modifiers;
    }

    public Map<String, Integer> getPassive() {
        return passive;
    }

    public void setPassive(Map<String, Integer> passive) {
        this.passive = passive;
    }

    public Map<String, Integer> getCombat() {
        return combat;
    }

    public void setCombat(Map<String, Integer> combat) {
        this.combat = combat;
    }
}
