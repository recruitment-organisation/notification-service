package recruitment.dev.notificationservice.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import recruitment.dev.notificationservice.dto.NotificationType;

@Converter
public class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {

    @Override
    public String convertToDatabaseColumn(NotificationType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public NotificationType convertToEntityAttribute(String databaseValue) {
        return databaseValue == null ? null : NotificationType.valueOf(databaseValue);
    }
}
