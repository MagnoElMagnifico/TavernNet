package tarvernnet.model;

import java.util.ArrayList;
import java.util.HashMap;

public class Stats {
    private HashMap<String, Integer> main;
    private HashMap<String, Integer> modifiers;
    private HashMap<String, Integer> passives;
    private HashMap<String, Integer> combat;
    private HashMap<String, String> skills;
    private ArrayList<String> languages;

    public Stats() {
    }

    public Stats(HashMap<String, Integer> main, HashMap<String,
                Integer> modifiers, HashMap<String, Integer> passives,
                 HashMap<String, Integer> combat, HashMap<String, String> skills,
                 ArrayList<String> languages) {
        this.main = main;
        this.modifiers = modifiers;
        this.passives = passives;
        this.combat = combat;
        this.skills = skills;
        this.languages = languages;
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

    public HashMap<String, Integer> getPassives() {
        return passives;
    }

    public void setPassives(HashMap<String, Integer> passives) {
        this.passives = passives;
    }

    public HashMap<String, Integer> getCombat() {
        return combat;
    }

    public void setCombat(HashMap<String, Integer> combat) {
        this.combat = combat;
    }

    public HashMap<String, String> getSkills() {
        return skills;
    }

    public void setSkills(HashMap<String, String> skills) {
        this.skills = skills;
    }

    public ArrayList<String> getLanguages() {
        return languages;
    }

    public void setLanguages(ArrayList<String> languages) {
        this.languages = languages;
    }
}
