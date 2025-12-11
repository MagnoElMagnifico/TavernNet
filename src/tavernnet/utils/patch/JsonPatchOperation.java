package tavernnet.utils.patch;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonPatchOperation(
    @JsonAlias("op") @NotNull @Valid JsonPatchOperationType operation,
    @NotNull @Valid JsonPointer path,
    @Valid JsonPointer from,
    @NotNull @Valid JsonNode value
) {}
