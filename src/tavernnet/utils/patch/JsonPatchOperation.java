package tavernnet.utils.patch;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonPatchOperation(@JsonAlias("op") JsonPatchOperationType operation, JsonPointer path, JsonPointer from, JsonNode value) { }
