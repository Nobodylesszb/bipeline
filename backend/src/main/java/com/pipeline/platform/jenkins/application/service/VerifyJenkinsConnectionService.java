package com.pipeline.platform.jenkins.application.service;

import com.pipeline.platform.jenkins.application.command.VerifyJenkinsConnectionCommand;
import com.pipeline.platform.jenkins.application.model.JenkinsConnectionVerificationView;
import com.pipeline.platform.jenkins.application.model.JenkinsVerification;
import com.pipeline.platform.jenkins.application.port.JenkinsClient;
import com.pipeline.platform.jenkins.domain.JenkinsConnection;
import com.pipeline.platform.jenkins.domain.JenkinsConnectionRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import com.pipeline.platform.source.domain.VerificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VerifyJenkinsConnectionService {

    @Autowired
    private JenkinsConnectionRepository jenkinsConnectionRepository;

    @Autowired
    private JenkinsClient jenkinsClient;

    @Autowired
    private ClockProvider clockProvider;

    @Transactional(rollbackFor = Exception.class)
    public JenkinsConnectionVerificationView verify(VerifyJenkinsConnectionCommand command) {
        JenkinsConnection connection = jenkinsConnectionRepository.findById(command.connectionId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Jenkins connection not found"
                ));

        JenkinsVerification verification = jenkinsClient.verify(connection);
        VerificationStatus status = verification.verified()
                ? VerificationStatus.VERIFIED
                : VerificationStatus.FAILED;
        JenkinsConnection saved = jenkinsConnectionRepository.save(connection.withVerification(
                status,
                clockProvider.now(),
                verification.message()
        ));
        if (!verification.verified()) {
            throw new BusinessException(
                    ErrorCode.JENKINS_CONNECTION_FAILED,
                    verification.message()
            );
        }
        return new JenkinsConnectionVerificationView(
                saved.verificationStatus(),
                saved.lastVerificationMessage(),
                saved.lastVerifiedAt()
        );
    }
}
