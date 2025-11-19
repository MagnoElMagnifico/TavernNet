package tavernnet.utils.patch;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonPatchOperation(@JsonAlias("op") JsonPatchOperationType operation, JsonPointer path, JsonPointer from, JsonNode value) { }
