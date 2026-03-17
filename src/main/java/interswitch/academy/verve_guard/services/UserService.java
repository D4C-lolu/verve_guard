package interswitch.academy.verve_guard.services;

import com.github.f4b6a3.ulid.UlidCreator;
import interswitch.academy.verve_guard.entities.Role;
import interswitch.academy.verve_guard.entities.User;
import interswitch.academy.verve_guard.exceptions.BadRequestException;
import interswitch.academy.verve_guard.exceptions.ConflictException;
import interswitch.academy.verve_guard.exceptions.NotFoundException;
import interswitch.academy.verve_guard.mapper.UserMapper;
import interswitch.academy.verve_guard.models.enums.UserStatus;
import interswitch.academy.verve_guard.models.request.ChangePasswordRequest;
import interswitch.academy.verve_guard.models.request.CreateUserRequest;
import interswitch.academy.verve_guard.models.request.UpdateUserRequest;
import interswitch.academy.verve_guard.models.response.UserResponse;
import interswitch.academy.verve_guard.repositories.RoleRepository;
import interswitch.academy.verve_guard.repositories.UserRepository;
import interswitch.academy.verve_guard.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new ConflictException("Phone already in use");
        }

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new NotFoundException("Role not found"));

        User user = User.builder()
                .id(UlidCreator.getUlid().toString())
                .firstname(request.firstname())
                .lastname(request.lastname())
                .othername(request.othername())
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .userStatus(UserStatus.ACTIVE)
                .role(role)
                .build();

        return userMapper.map(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        User user = findActiveUserById(userId);
        Map<String, String> conflicts = new LinkedHashMap<>();

        if (!user.getEmail().equals(request.email())) {
            if (userRepository.existsByEmail(request.email())) {
                conflicts.put("email", "Email already in use");
            }
        }

        if (!user.getPhone().equals(request.phone())) {
            if (userRepository.existsByPhone(request.phone())) {
                conflicts.put("phone", "Phone already in use");
            }
        }

        if (!conflicts.isEmpty()) {
            throw new ConflictException(String.join(", ", conflicts.values()));
        }

        user.setFirstname(request.firstname());
        user.setLastname(request.lastname());
        if (request.othername() != null) user.setOthername(request.othername());
        user.setEmail(request.email());
        user.setPhone(request.phone());

        return userMapper.map(userRepository.save(user));
    }

    @Transactional
    public void changeUserStatus(String userId, UserStatus status) {
        User user = findActiveUserById(userId);
        user.setUserStatus(status);
        userRepository.save(user);
    }

    @Transactional
    public void changeUserRole(String userId, String roleId) {
        User user = findActiveUserById(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found"));
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String userId) {

        User user = findActiveUserById(userId);
        user.softDelete();
        userRepository.save(user);
    }

    @Transactional
    public void deleteCurrentUser() {
        String deletedBy = SecurityUtil.getCurrentUserId();
        deleteUser(deletedBy);
    }

    public UserResponse getUserById(String userId) {
        return userMapper.map(findActiveUserById(userId));
    }

    public List<UserResponse> getAllUsers() {
        return userMapper.map(userRepository.findAllByDeletedAtIsNull());
    }

    private User findActiveUserById(String userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public Page<UserResponse> getAllUsers(int page, int size, String sortField, Sort.Direction sortDirection) {
        Sort sort = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return userRepository.findAllByDeletedAtIsNull(pageable)
                .map(userMapper::map);
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = findActiveUserById(userId);

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}