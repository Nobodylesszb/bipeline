package com.pipeline.platform.jenkins.application.service;

import com.pipeline.platform.jenkins.application.command.CreateJenkinsConnectionCommand;
import com.pipeline.platform.jenkins.application.model.JenkinsConnectionView;
import com.pipeline.platform.jenkins.domain.JenkinsConnection;
import com.pipeline.platform.jenkins.domain.JenkinsConnectionRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateJenkinsConnectionService {

    @Autowired
    private JenkinsConnectionRepository jenkinsConnectionRepository;

    @Autowired
    private ClockProvider clockProvider;

    @Transactional(rollbackFor = Exception.class)
    public JenkinsConnectionView create(CreateJenkinsConnectionCommand command) {
        if (jenkinsConnectionRepository.existsByName(command.name())) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Jenkins connection name already exists"
            );
        }
        JenkinsConnection connection = JenkinsConnection.create(
                command.name(),
                command.baseUrl(),
                command.username(),
                command.apiToken(),
                clockProvider.now()
        );
        return JenkinsConnectionView.from(jenkinsConnectionRepository.save(connection));
    }
}
