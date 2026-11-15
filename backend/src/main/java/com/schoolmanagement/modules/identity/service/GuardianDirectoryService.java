package com.schoolmanagement.modules.identity.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.schoolmanagement.modules.identity.dto.GuardianProfileResponse;
import com.schoolmanagement.modules.identity.entity.GuardianProfile;
import com.schoolmanagement.modules.identity.repository.GuardianProfileRepository;

@Service
public class GuardianDirectoryService {

    private final GuardianProfileRepository guardianRepository;

    public GuardianDirectoryService(
            GuardianProfileRepository guardianRepository
    ) {
        this.guardianRepository = guardianRepository;
    }

    @Transactional(readOnly = true)
    public List<GuardianProfileResponse> getAllGuardians() {

        return guardianRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private GuardianProfileResponse toResponse(
            GuardianProfile guardian
    ) {
        return new GuardianProfileResponse(
                guardian.getId(),
                guardian.getUserId(),
                guardian.getAlternatePhone()
        );
    }
}