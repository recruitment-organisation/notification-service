package recruitment.dev.notificationservice.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OfferEmailServiceTest {

    @Test
    void sendsTheEmploymentOfferThroughSmtpWhenEnabled() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        OfferEmailService service = new OfferEmailService(mailSender, true, "jobs@example.com");

        EmailDeliveryResult result = service.sendOffer(
                "candidate@example.com",
                "Votre offre d'emploi",
                "Félicitations, votre candidature est retenue."
        );

        ArgumentCaptor<SimpleMailMessage> email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(email.capture());
        assertThat(result).isEqualTo(EmailDeliveryResult.SENT);
        assertThat(email.getValue().getFrom()).isEqualTo("jobs@example.com");
        assertThat(email.getValue().getTo()).containsExactly("candidate@example.com");
        assertThat(email.getValue().getSubject()).isEqualTo("Votre offre d'emploi");
    }

    @Test
    void doesNotContactSmtpWhenEmailDeliveryIsDisabled() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        OfferEmailService service = new OfferEmailService(mailSender, false, "jobs@example.com");

        assertThat(service.sendOffer("candidate@example.com", "Offre", "Message"))
                .isEqualTo(EmailDeliveryResult.DISABLED);
        verifyNoInteractions(mailSender);
    }
}
