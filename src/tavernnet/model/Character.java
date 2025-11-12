package tavernnet.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotBlank;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tavernnet.utils.ObjectIdSerializer;
import tavernnet.utils.ValidObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;


/*public record Character (
    @Id
    String id,
    String name,
    String user,
    String biography,
    String alignment,
    LocalDateTime date,
    Stats stats,
    ArrayList<Action> actions
){}*/
@Document(collection = "characters")
public class Character {
    /** Formato esperado del usuario al hacer POST para crear un personaje */
    public record UserInputCharacter (
        @NotBlank(message = "Name must be not null or blank")
        String name,
        @NotBlank(message = "User must be not null or blank")
        String user,
        String biography,
        String race,
        ArrayList<String> languages,
        String alignment,
        Stats stats,
        ArrayList<Action> actions
    ) {}

    // Datos internos
    @Id
    @JsonSerialize(using = ObjectIdSerializer.class)
    private ObjectId id;
    private String name;
    private String user;
    private String biography;
    private String race;
    private ArrayList<String> languages;
    private String alignment;
    private Stats stats;
    private ArrayList<Action> actions;
    private LocalDateTime date;

    public Character(){}

    public Character(ObjectId id, String name, String user, String biography,
                     String race, ArrayList<String> languages, String alignment,
                     Stats stats, ArrayList<Action> actions, LocalDateTime date) {
        this.id = id;
        this.name = name;
        this.user = user;
        this.biography = biography;
        this.race = race;
        this.languages = languages;
        this.alignment = alignment;
        this.stats = stats;
        this.actions = actions;
        this.date = date;
    }

    /** Creacion de un personaje por el usuario */
    public Character(Character.UserInputCharacter character, String author) {
        // Dejar el ID a null hará que la base de datos asigne uno automáticamente
        this(null, character.name, author, character.biography, character.race,
            character.languages, character.alignment, character.stats,
            character.actions, LocalDateTime.now());
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public ArrayList<String> getLanguages() {
        return languages;
    }

    public void setLanguages(ArrayList<String> languages) {
        this.languages = languages;
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
