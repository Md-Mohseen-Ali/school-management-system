package com.schoolmanagement.modules.identity.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.schoolmanagement.modules.identity.dto.GuardianProfileResponse;
import com.schoolmanagement.modules.identity.entity.AppUser;
import com.schoolmanagement.modules.identity.entity.GuardianProfile;
import com.schoolmanagement.modules.identity.repository.AppUserRepository;
import com.schoolmanagement.modules.identity.repository.GuardianProfileRepository;

@Service
public class GuardianDirectoryService {

    private final GuardianProfileRepository guardianRepository;
    private final AppUserRepository appUserRepository;

    public GuardianDirectoryService(
            GuardianProfileRepository guardianRepository,
            AppUserRepository appUserRepository) {
        this.guardianRepository = guardianRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<GuardianProfileResponse> getAllGuardians() {
        return guardianRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private GuardianProfileResponse toResponse(GuardianProfile guardian) {
        AppUser user = appUserRepository.findById(guardian.getUserId()).orElse(null);
        return new GuardianProfileResponse(
                guardian.getId(),
                guardian.getUserId(),
                user == null ? null : user.getFullName(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getPhone(),
                guardian.getAlternatePhone()
        );
    }
}
