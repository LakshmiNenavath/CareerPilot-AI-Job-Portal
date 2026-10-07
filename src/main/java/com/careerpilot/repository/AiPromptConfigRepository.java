package com.careerpilot.repository;

import com.careerpilot.model.AiPromptConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiPromptConfigRepository extends JpaRepository<AiPromptConfig, Long> {
    Optional<AiPromptConfig> findByPromptKey(String promptKey);
}
