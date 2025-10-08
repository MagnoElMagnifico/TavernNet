package tarvernnet.model;

public class Character {
    private String name;
    private String biography;
    private String alignment;
    private Stats stats;

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
}
