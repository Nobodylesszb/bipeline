package com.pipeline.platform.project.application.service;

import java.util.List;

import com.pipeline.platform.project.application.model.ProjectGitRepositoryView;
import com.pipeline.platform.project.application.model.ProjectView;
import com.pipeline.platform.project.domain.ProjectGitRepositoryRepository;
import com.pipeline.platform.project.domain.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListProjectsService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectGitRepositoryRepository projectGitRepositoryRepository;

    @Transactional(readOnly = true)
    public List<ProjectView> findAll() {
        return projectRepository.findAll()
                .stream()
                .map(project -> ProjectView.from(
                        project,
                        projectGitRepositoryRepository.findByProjectId(project.id())
                                .map(ProjectGitRepositoryView::from)
                                .orElse(null)
                ))
                .toList();
    }
}
