package tavernnet.utils;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ObjectIdSerializer extends JsonSerializer<ObjectId> {
    private static final Logger log = LoggerFactory.getLogger(ObjectIdSerializer.class);

    @Override
    public void serialize(ObjectId value, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        // TODO: esto nunca se ejecuta
        log.debug("Serialize: {} -> {}", value, value.toHexString());
        gen.writeString(value.toHexString());
    }
}
