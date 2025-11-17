package tavernnet.model;

import jakarta.validation.constraints.NotBlank;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Collection;

@Document(collection = "characters")
public class Character {
    // TODO: validacion

    /** DTO para el POST de crear un personaje */
    public record CreationRequest(
        @NotBlank(message = "Name must be not null or blank")
        String name,
        @NotBlank(message = "User must be not null or blank")
        String user,
        String biography,
        String race,
        Collection<String> languages,
        String alignment,
        Stats stats,
        Collection<Action> actions
    ) {}

    public record Response (
        String id,
        String name,
        String user,
        String biography,
        String race,
        Collection<String> languages,
        String alignment,
        Stats stats,
        Collection<Action> actions,
        LocalDateTime date
    ) {}

    @Id
    private ObjectId id;
    private String name;
    private String user;
    private String biography;
    private String race;
    private Collection<String> languages;
    private String alignment;
    private Stats stats;
    private Collection<Action> actions;
    private LocalDateTime date;

    public Character(){}

    public Character(ObjectId id, String name, String user, String biography,
                     String race, Collection<String> languages, String alignment,
                     Stats stats, Collection<Action> actions, LocalDateTime date) {
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
    public Character(CreationRequest character, String author) {
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

    public Collection<String> getLanguages() {
        return languages;
    }

    public void setLanguages(Collection<String> languages) {
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

    public Collection<Action> getActions() {
        return actions;
    }

    public void setActions(Collection<Action> actions) {
        this.actions = actions;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
