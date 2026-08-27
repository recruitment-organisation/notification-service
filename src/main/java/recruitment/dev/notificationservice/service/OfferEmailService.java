package recruitment.dev.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OfferEmailService {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public OfferEmailService(
            JavaMailSender mailSender,
            @Value("${notifications.email.enabled:false}") boolean enabled,
            @Value("${notifications.email.from:no-reply@recruitment.local}") String from
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public EmailDeliveryResult sendOffer(String recipient, String subject, String message) {
        if (!enabled) {
            log.warn("Employment-offer email is disabled; in-app notification will still be delivered");
            return EmailDeliveryResult.DISABLED;
        }
        if (recipient == null || recipient.isBlank()) {
            log.error("Employment-offer email cannot be sent without a recipient");
            return EmailDeliveryResult.FAILED;
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(from);
        email.setTo(recipient);
        email.setSubject(subject);
        email.setText(message);
        try {
            mailSender.send(email);
            log.info("Employment-offer email sent to {}", recipient);
            return EmailDeliveryResult.SENT;
        } catch (MailException exception) {
            log.error("Employment-offer email delivery failed for {}", recipient, exception);
            return EmailDeliveryResult.FAILED;
        }
    }
}
