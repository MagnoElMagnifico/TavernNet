package tarvernnet.model;

import java.util.ArrayList;

public class Group {
    private int id;
    private String name;
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
}
