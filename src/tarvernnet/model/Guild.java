package tarvernnet.model;

import org.springframework.resilience.annotation.EnableResilientMethods;

import java.util.ArrayList;

public class Guild {
    private String name;
    private String description;
    private ArrayList<Character> characters;

    public Guild(String name, String description, ArrayList<Character> characters) {
        this.name = name;
        this.description = description;
        this.characters = characters;
    }
}
