package com.pipeline.platform.project.application.service;

import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceRepository;
import com.pipeline.platform.source.domain.VerificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class CodeSourceGuard {

    @Autowired
    private CodeSourceRepository codeSourceRepository;

    CodeSource requireVerified(Long codeSourceId) {
        CodeSource codeSource = codeSourceRepository.findById(codeSourceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Code source not found"
                ));
        if (codeSource.verificationStatus() != VerificationStatus.VERIFIED) {
            throw new BusinessException(
                    ErrorCode.CODE_SOURCE_VERIFICATION_FAILED,
                    "Code source must be verified before use"
            );
        }
        return codeSource;
    }
}
