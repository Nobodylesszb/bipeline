package com.pipeline.platform.source.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCodeSourceRepository implements CodeSourceRepository {

    private final SpringDataCodeSourceRepository springDataRepository;

    public JpaCodeSourceRepository(SpringDataCodeSourceRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public boolean existsByName(String name) {
        return springDataRepository.existsByName(name);
    }

    @Override
    public CodeSource save(CodeSource codeSource) {
        return springDataRepository.save(CodeSourceJpaEntity.from(codeSource)).toDomain();
    }

    @Override
    public Optional<CodeSource> findById(UUID id) {
        return springDataRepository.findById(id).map(CodeSourceJpaEntity::toDomain);
    }

    @Override
    public List<CodeSource> findAll() {
        return springDataRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(CodeSourceJpaEntity::toDomain)
                .toList();
    }
}
