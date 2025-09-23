package com.mserapinas.boardgame.userservice.repository;

import com.mserapinas.boardgame.userservice.model.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {
    
    List<Label> findByUserIdAndNameIn(Long userId, Set<String> names);
}