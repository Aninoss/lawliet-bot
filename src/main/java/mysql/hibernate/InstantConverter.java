package mysql.hibernate;

import javax.persistence.AttributeConverter;
import java.time.Instant;

public class InstantConverter implements AttributeConverter<Instant, String> {

    @Override
    public String convertToDatabaseColumn(Instant attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.toString();
    }

    @Override
    public Instant convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Instant.parse(dbData);
    }

}
