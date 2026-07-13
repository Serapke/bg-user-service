package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.dto.request.CreatePlayerGroupRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdatePlayerGroupRequest;
import com.mserapinas.boardgame.userservice.dto.response.PlayerGroupDto;
import com.mserapinas.boardgame.userservice.exception.PlayerGroupAccessForbiddenException;
import com.mserapinas.boardgame.userservice.exception.PlayerGroupNotFoundException;
import com.mserapinas.boardgame.userservice.model.PlayerGroup;
import com.mserapinas.boardgame.userservice.model.User;
import com.mserapinas.boardgame.userservice.repository.PlayerGroupRepository;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PlayerGroupService {

    private final PlayerGroupRepository playerGroupRepository;
    private final UserRepository userRepository;

    public PlayerGroupService(PlayerGroupRepository playerGroupRepository, UserRepository userRepository) {
        this.playerGroupRepository = playerGroupRepository;
        this.userRepository = userRepository;
    }

    public List<PlayerGroupDto> listGroups(Long userId) {
        return playerGroupRepository.findByCreatorIdWithMembers(userId).stream()
            .map(PlayerGroupDto::from)
            .toList();
    }

    @Transactional
    public PlayerGroupDto createGroup(Long userId, CreatePlayerGroupRequest request) {
        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<User> members = resolveMembers(request.memberIds());

        PlayerGroup group = new PlayerGroup();
        group.setCreator(creator);
        group.setName(request.name());
        group.setMembers(members);

        PlayerGroup saved;
        try {
            saved = playerGroupRepository.save(group);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("A group named '" + request.name() + "' already exists");
        }
        return PlayerGroupDto.from(
            playerGroupRepository.findByIdWithMembers(saved.getId())
                .orElseThrow(() -> new PlayerGroupNotFoundException(saved.getId()))
        );
    }

    @Transactional
    public PlayerGroupDto updateGroup(Long userId, Long groupId, UpdatePlayerGroupRequest request) {
        PlayerGroup group = playerGroupRepository.findByIdWithMembers(groupId)
            .orElseThrow(() -> new PlayerGroupNotFoundException(groupId));

        if (!group.getCreator().getId().equals(userId)) {
            throw new PlayerGroupAccessForbiddenException(groupId, userId);
        }

        group.setName(request.name());
        group.setMembers(resolveMembers(request.memberIds()));
        try {
            playerGroupRepository.save(group);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("A group named '" + request.name() + "' already exists");
        }

        return PlayerGroupDto.from(
            playerGroupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new PlayerGroupNotFoundException(groupId))
        );
    }

    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        PlayerGroup group = playerGroupRepository.findById(groupId)
            .orElseThrow(() -> new PlayerGroupNotFoundException(groupId));

        if (!group.getCreator().getId().equals(userId)) {
            throw new PlayerGroupAccessForbiddenException(groupId, userId);
        }

        playerGroupRepository.deleteById(groupId);
    }

    private Set<User> resolveMembers(Set<Long> memberIds) {
        Set<User> members = new HashSet<>(userRepository.findAllById(memberIds));
        if (members.size() != memberIds.size()) {
            throw new IllegalArgumentException("One or more member IDs are invalid");
        }
        return members;
    }
}
