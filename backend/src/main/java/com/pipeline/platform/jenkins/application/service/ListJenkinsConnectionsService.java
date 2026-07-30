package com.pipeline.platform.jenkins.application.service;

import java.util.List;

import com.pipeline.platform.jenkins.application.model.JenkinsConnectionView;
import com.pipeline.platform.jenkins.domain.JenkinsConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListJenkinsConnectionsService {

    @Autowired
    private JenkinsConnectionRepository jenkinsConnectionRepository;

    @Transactional(readOnly = true)
    public List<JenkinsConnectionView> findAll() {
        return jenkinsConnectionRepository.findAll()
                .stream()
                .map(JenkinsConnectionView::from)
                .toList();
    }
}
