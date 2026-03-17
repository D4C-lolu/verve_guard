package interswitch.academy.verve_guard.services;

import com.github.f4b6a3.ulid.UlidCreator;
import interswitch.academy.verve_guard.entities.Permission;
import interswitch.academy.verve_guard.exceptions.ConflictException;
import interswitch.academy.verve_guard.exceptions.NotFoundException;
import interswitch.academy.verve_guard.mapper.PermissionMapper;
import interswitch.academy.verve_guard.models.request.CreatePermissionRequest;
import interswitch.academy.verve_guard.models.response.PermissionResponse;
import interswitch.academy.verve_guard.repositories.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByName(request.name())) {
            throw new ConflictException("Permission already exists");
        }

        Permission permission = Permission.builder()
                .id(UlidCreator.getUlid().toString())
                .name(request.name())
                .description(request.description())
                .build();

        return permissionMapper.map(permissionRepository.save(permission));
    }

    public PermissionResponse getPermissionById(String permissionId) {
        return permissionMapper.map(permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NotFoundException("Permission not found")));
    }

    public List<PermissionResponse> getAllPermissions() {
        return permissionMapper.map(permissionRepository.findAll());
    }
}