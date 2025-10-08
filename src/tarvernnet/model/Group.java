package tarvernnet.model;

import java.util.ArrayList;

public class Group {
    private int id;
    private String name;
    private String description;
    private Guild guild;
    private ArrayList<String> characters;
    private ArrayList<String> admins;
    private ArrayList<Message> messages;

    public Group(int id, String name, ArrayList<String> characters,
                 ArrayList<String> admins, ArrayList<Message> messages) {
        this.id = id;
        this.name = name;
        this.characters = characters;
        this.admins = admins;
        this.messages = messages;
    }

    public Group(int id, String name, String description, ArrayList<String> characters,
                 ArrayList<String> admins, ArrayList<Message> messages) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.characters = characters;
        this.admins = admins;
        this.messages = messages;
    }

    public Group(int id, String name, String description, Guild guild,
                 ArrayList<String> characters, ArrayList<String> admins,
                 ArrayList<Message> messages) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.guild = guild;
        this.characters = characters;
        this.admins = admins;
        this.messages = messages;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<String> getCharacters() {
        return characters;
    }

    public void setCharacters(ArrayList<String> characters) {
        this.characters = characters;
    }

    public ArrayList<String> getAdmins() {
        return admins;
    }

    public void setAdmins(ArrayList<String> admins) {
        this.admins = admins;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public Guild getGuild() {
        return guild;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }
}
