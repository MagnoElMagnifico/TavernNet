package tavernnet.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.bson.types.ObjectId;

public class ObjectIdValidator implements ConstraintValidator<ValidObjectId, Object> {
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null)
            return true;

        if (value instanceof String str) {
            return ObjectId.isValid(str);
        }

        return value instanceof ObjectId; // un ObjectId ya es válido, cualquier otro tipo de datos no
    }
}
