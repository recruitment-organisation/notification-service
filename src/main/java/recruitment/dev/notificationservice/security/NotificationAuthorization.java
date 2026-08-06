package recruitment.dev.notificationservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("notificationAuthorization")
public class NotificationAuthorization {

    private static final String WORKFLOW_CLIENT_ID = "workflow-service";
    private static final String SHARED_SERVICE_CLIENT_ID = "spring-service";

    public boolean isWorkflowService(Authentication authentication) {
        return authentication != null
                && authentication.getPrincipal() instanceof Jwt jwt
                && (WORKFLOW_CLIENT_ID.equals(jwt.getClaimAsString("azp"))
                || SHARED_SERVICE_CLIENT_ID.equals(jwt.getClaimAsString("azp")));
    }
}
