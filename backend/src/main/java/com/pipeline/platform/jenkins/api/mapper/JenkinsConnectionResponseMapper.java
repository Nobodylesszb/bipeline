package com.pipeline.platform.jenkins.api.mapper;

import com.pipeline.platform.jenkins.api.response.JenkinsConnectionResponse;
import com.pipeline.platform.jenkins.api.response.JenkinsConnectionVerificationResponse;
import com.pipeline.platform.jenkins.application.model.JenkinsConnectionVerificationView;
import com.pipeline.platform.jenkins.application.model.JenkinsConnectionView;
import com.pipeline.platform.shared.security.SecretMasker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JenkinsConnectionResponseMapper {

    @Autowired
    private SecretMasker secretMasker;

    public JenkinsConnectionResponse toResponse(JenkinsConnectionView view) {
        SecretMasker.MaskedSecret maskedSecret = secretMasker.mask(view.apiTokenPlain());
        return new JenkinsConnectionResponse(
                view.id(),
                view.name(),
                view.baseUrl(),
                view.username(),
                maskedSecret.masked(),
                maskedSecret.lastFour(),
                view.verificationStatus(),
                view.lastVerifiedAt(),
                view.lastVerificationMessage(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public JenkinsConnectionVerificationResponse toVerificationResponse(JenkinsConnectionVerificationView view) {
        return new JenkinsConnectionVerificationResponse(
                view.status(),
                view.message(),
                view.verifiedAt()
        );
    }
}
