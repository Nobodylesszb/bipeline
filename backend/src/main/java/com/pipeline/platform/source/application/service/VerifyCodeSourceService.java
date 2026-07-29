package com.pipeline.platform.source.application.service;

import java.util.List;

import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import com.pipeline.platform.source.application.command.VerifyCodeSourceCommand;
import com.pipeline.platform.source.application.model.CodeSourceVerificationView;
import com.pipeline.platform.source.application.model.GitProviderVerification;
import com.pipeline.platform.source.application.port.GitProviderClient;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceRepository;
import com.pipeline.platform.source.domain.VerificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VerifyCodeSourceService {

    @Autowired
    private CodeSourceRepository codeSourceRepository;

    @Autowired
    private List<GitProviderClient> gitProviderClients;

    @Autowired
    private ClockProvider clockProvider;

    @Transactional
    public CodeSourceVerificationView verify(VerifyCodeSourceCommand command) {
        CodeSource codeSource = codeSourceRepository.findById(command.codeSourceId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Code source not found"
                ));

        GitProviderClient gitProviderClient = gitProviderClients.stream()
                .filter(client -> client.supports(codeSource.provider()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CODE_SOURCE_VERIFICATION_FAILED,
                        "Code source provider is not supported yet"
                ));

        GitProviderVerification verification = gitProviderClient.verify(
                codeSource,
                command.repositoryPath()
        );
        VerificationStatus status = verification.verified()
                ? VerificationStatus.VERIFIED
                : VerificationStatus.FAILED;

        CodeSource verifiedCodeSource = codeSource.withVerification(
                status,
                clockProvider.now(),
                verification.message()
        );
        codeSourceRepository.save(verifiedCodeSource);

        if (!verification.verified()) {
            throw new BusinessException(
                    ErrorCode.CODE_SOURCE_VERIFICATION_FAILED,
                    verification.message()
            );
        }

        return new CodeSourceVerificationView(
                status,
                verification.message(),
                verification.capabilities(),
                verifiedCodeSource.lastVerifiedAt()
        );
    }
}
