package com.pipeline.platform.source.api;

import com.pipeline.platform.shared.security.SecretMasker;
import com.pipeline.platform.source.application.CodeSourceView;
import org.springframework.stereotype.Component;

@Component
public class CodeSourceResponseMapper {

    private final SecretMasker secretMasker;

    public CodeSourceResponseMapper(SecretMasker secretMasker) {
        this.secretMasker = secretMasker;
    }

    public CodeSourceResponse toResponse(CodeSourceView view) {
        SecretMasker.MaskedSecret maskedSecret = secretMasker.mask(view.secretPlain());
        return new CodeSourceResponse(
                view.id(),
                view.name(),
                view.provider(),
                view.baseUrl(),
                view.authType(),
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
}
