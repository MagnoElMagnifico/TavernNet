package tarvernnet.model;

import java.util.ArrayList;

public class Character {
    private String name;
    private String biography;
    private String alignment;
    private Stats stats;
    private ArrayList<Action> actions;

    public Character(String name) {
        this.name = name;
    }

    public Character(String name, String biography, String alignment) {
        this.name = name;
        this.biography = biography;
        this.alignment = alignment;
    }

    public Character(String name, String biography, String alignment, Stats stats) {
        this.name = name;
        this.biography = biography;
        this.alignment = alignment;
        this.stats = stats;
    }

    public Character(String name, String biography, String alignment, Stats stats, ArrayList<Action> actions) {
        this.name = name;
        this.biography = biography;
        this.alignment = alignment;
        this.stats = stats;
        this.actions = actions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getAlignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public ArrayList<Action> getActions() {
        return actions;
    }

    public void setActions(ArrayList<Action> actions) {
        this.actions = actions;
    }
}
