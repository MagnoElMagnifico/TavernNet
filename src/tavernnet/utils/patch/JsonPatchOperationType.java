package tavernnet.utils.patch;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum JsonPatchOperationType {

    ADD, REMOVE, REPLACE, COPY, MOVE;

    @JsonCreator
    public static JsonPatchOperationType forValue(String value) {
        return JsonPatchOperationType.valueOf(value.toUpperCase());
    }
}
