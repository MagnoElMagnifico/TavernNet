package tavernnet.model;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Document(collection = "characters")
/*public record Character (
    String id,
    String name,
    String biography,
    String alignment,
    Stats stats,
    ArrayList<Action> actions
){}*/
public class Character {
    private final String id;
    private String name;
    private String biography;
    private String alignment;
    private Stats stats;
    private ArrayList<Action> actions;


    public Character(String id, String name, String biography, String alignment, Stats stats, ArrayList<Action> actions) {
        this.id = id;
        this.name = name;
        this.biography = biography;
        this.alignment = alignment;
        this.stats = stats;
        this.actions = actions;
    }

    public String getId() {
        return id;
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
