package com.pipeline.platform.source.api.mapper;

import com.pipeline.platform.shared.security.SecretMasker;
import com.pipeline.platform.source.api.response.CodeSourceResponse;
import com.pipeline.platform.source.application.model.CodeSourceView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CodeSourceResponseMapper {

    @Autowired
    private SecretMasker secretMasker;

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
