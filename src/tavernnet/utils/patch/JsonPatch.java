package tavernnet.utils.patch;

import tavernnet.utils.patch.exceptions.*;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

public class JsonPatch {
    public static JsonNode apply(List<JsonPatchOperation> patchOperations, JsonNode rootDocument) throws tavernnet.utils.patch.exceptions.JsonPatchFailedException {
        for (JsonPatchOperation instruction : patchOperations) {
            rootDocument = switch (instruction.operation()) {
                case ADD -> add(instruction.path(), instruction.value(), rootDocument);
                case REMOVE -> remove(instruction.path(), rootDocument);
                case REPLACE -> replace(instruction.path(), instruction.value(), rootDocument);
                case COPY -> copy(instruction.path(), instruction.from(), rootDocument);
                case MOVE -> move(instruction.path(), instruction.from(), rootDocument);
            };
        }
        return rootDocument;
    }

    public static JsonNode add(JsonPointer path, JsonNode value, JsonNode document) throws JsonPatchFailedException {
        return JsonPatchUtil.put(value, path, document);
    }

    public static JsonNode copy(JsonPointer path, JsonPointer from, JsonNode document) throws JsonPatchFailedException {
        return JsonPatchUtil.put(JsonPatchUtil.at(document, from), path, document);
    }

    public static JsonNode move(JsonPointer path, JsonPointer from, JsonNode document) throws JsonPatchFailedException {
        return JsonPatchUtil.remove(from, JsonPatchUtil.put(JsonPatchUtil.at(document, from), path, document));
    }

    public static JsonNode remove(JsonPointer path, JsonNode document) throws JsonPatchFailedException {
        return JsonPatchUtil.remove(path, document);
    }

    public static JsonNode replace(JsonPointer path, JsonNode value, JsonNode document) throws JsonPatchFailedException {
        return JsonPatchUtil.put(value, path, JsonPatchUtil.remove(path, document));
    }
}
