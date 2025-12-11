package tavernnet.utils.patch;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class JsonPatchUtil {

    public static JsonNode remove(JsonPointer pointer, JsonNode node) {
        JsonPointer element = pointer.last(); // Last element from pointer
        JsonPointer path = pointer.head(); // Prefix from pointer

        // Get object to modify
        JsonNode target = node.at(path);

        // If the node is an array, remove from it
        if (target.isArray()) {
            // remove from position
            if (element.mayMatchElement()) {
                ((ArrayNode) target).remove(element.getMatchingIndex());
                // remove last
            } else if (element.mayMatchProperty() && element.getMatchingProperty().equals("-")) {
                ((ArrayNode) target).remove(target.size() - 1);
            }
            // If the node is an object, remove property
        } else if (target.isObject()){
            if(element.mayMatchProperty() && !element.getMatchingProperty().trim().equals("-"))
                ((ObjectNode) target).remove(element.getMatchingProperty());
        }

        return node;
    }

    public static JsonNode put(JsonNode value, JsonPointer pointer, JsonNode node) {
        JsonPointer element = pointer.last(); // Last element from pointer
        JsonPointer path = pointer.head(); // Prefix from pointer

        // Get object to modify
        JsonNode target = node.at(path);

        // If the node is an array, add to it
        if (target.isArray()) {
            // add at position
            if (element.mayMatchElement()) {
                ((ArrayNode) target).insert(element.getMatchingIndex(), value);
                // add last
            } else if (element.mayMatchProperty() && element.getMatchingProperty().equals("-")) {
                ((ArrayNode) target).insert(target.size(), value);
            }
            // If the node is an object, set property
        } else if (target.isObject()) {
            if (element.mayMatchProperty() && !element.getMatchingProperty().trim().equals("-"))
                ((ObjectNode) target).set(element.getMatchingProperty(), value);
        }

        return node;
    }

    public static JsonNode at(JsonNode node, JsonPointer pointer) {
        JsonPointer element = pointer.last();
        JsonPointer path = pointer.head();

        JsonNode target = node.at(path);

        if (target.isArray()){
            if (element.mayMatchElement())
                return target.get(element.getMatchingIndex());
            else if (element.mayMatchProperty())
                return target.get(target.size() - 1);
        } else if (target.isObject()) {
            if (element.mayMatchProperty() && !element.getMatchingProperty().trim().equals("-"))
                return target.get(element.getMatchingProperty());
        }

        return target;
    }
}

