package tavernnet.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PatchUtils {

    private ObjectMapper mapper;

    //public PatchUtils(ObjectMapper mapper) {
    //    this.mapper = mapper;
    //}

    @SuppressWarnings("unchecked")
    public <T> T applyPatch(T data, List<Map<String, Object>> updates) throws JsonPatchException {
        // 1. Converter a lista de mapas a un obxecto JsonPatch
        JsonPatch operations = mapper.convertValue(updates, JsonPatch.class);

        // 2. Converter o obxecto de dominio a JsonNode
        JsonNode json = mapper.convertValue(data, JsonNode.class);

        // 3. Aplicar as operacións ao JsonNode
        JsonNode updatedJson = operations.apply(json);

        // 4. Converter o JsonNode modificado de volta ao obxecto de dominio
        return (T) mapper.convertValue(updatedJson, data.getClass());
    }
}
