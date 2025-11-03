package tavernnet.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Document(collection = "characters")
/*public record Character (
    @Id
    String id,
    String name,
    String biography,
    String alignment,
    LocalDateTime date,
    Stats stats,
    ArrayList<Action> actions
){}*/
public class Character {
    @Id
    private final String id;
    private String name;
    private String biography;
    private String alignment;
    private Stats stats;
    private ArrayList<Action> actions;
    private LocalDateTime date;


    public Character(String id, String name, String biography, String alignment, Stats stats, ArrayList<Action> actions, LocalDateTime date) {
        this.id = id;
        this.name = name;
        this.biography = biography;
        this.alignment = alignment;
        this.stats = stats;
        this.actions = actions;
        this.date = date;
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

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
