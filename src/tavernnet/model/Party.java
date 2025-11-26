package tavernnet.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collection;
import java.util.List;

@Document(collection = "parties")
public class Party {
    @Id
    private final String name;
    private Collection<Character> characters;
    private User DM;

    public Party(String name, Collection<Character> characters, User DM) {
        this.name = name;
        this.characters = characters;
        this.DM = DM;
    }

    public String getName() {
        return name;
    }

    public Collection<Character> getCharacterList() {
        return characters;
    }

    public void setCharacterList(Collection<Character> characterList) {
        this.characters = characterList;
    }

    public User getDM() {
        return DM;
    }

    public void setDM(User DM) {
        this.DM = DM;
    }
}
