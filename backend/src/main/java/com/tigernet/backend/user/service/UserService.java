package com.tigernet.backend.user.service;

import com.tigernet.backend.audit.repository.AuditLogRepository;
import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.security.SecurityUtils;
import com.tigernet.backend.user.dto.request.UserCreationRequest;
import com.tigernet.backend.user.dto.request.UserUpdateRequest;
import com.tigernet.backend.user.dto.response.UserDeleteResponse;
import com.tigernet.backend.user.dto.response.UserResponse;
import com.tigernet.backend.user.entity.RoleEntity;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.mapper.UserMapper;
import com.tigernet.backend.user.repository.RoleRepository;
import com.tigernet.backend.user.repository.UserPagePermissionRepository;
import com.tigernet.backend.user.repository.UserRepository;
import com.tigernet.backend.warehouse.repository.UserWarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class UserService {
    private static final String ADMIN_ROLE = "ADMIN";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserWarehouseRepository userWarehouseRepository;

    @Autowired
    private UserPagePermissionRepository pagePermissionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    public UserResponse createUser(UserCreationRequest request){
        if(userRepository.existsByUsername(request.getUsername().trim())) {
            throw new AppException(ErrorCode.USERNAME_EXISTED);
        }

        UserEntity user = userMapper.toUser(request);
        user.setUsername(request.getUsername().trim());
        user.setEmail(blankToNull(request.getEmail()));
        user.setIsActive(request.getIsActive() == null || request.getIsActive()); // mặc định đang hoạt động
        user.setCreatedAt(LocalDateTime.now()); // server tự set thời điểm tạo
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // mã hóa BCrypt trước khi lưu
        user.setRole(findRoleById(request.getRoleId()));

        UserEntity saved = userRepository.save(user);
        auditLogService.log("CREATE_USER", "Users", saved.getUserId(), snapshot(saved));
        return userMapper.toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(){
        return userMapper.toUserResponses(userRepository.findAll());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer userId){
        return userMapper.toUserResponse(findUserById(userId));
    }

    public UserResponse updateUser(Integer userId, UserUpdateRequest request){
        UserEntity user = findUserById(userId);
        Map<String, Object> before = snapshot(user);
        RoleEntity newRole = request.getRoleId() != null ? findRoleById(request.getRoleId()) : user.getRole();

        // Quản trị không tự khóa hoặc tự bỏ quyền ADMIN của chính mình (tránh mất quyền quản trị)
        if(isCurrentUser(userId) && (Boolean.FALSE.equals(request.getIsActive())
                || (newRole == null || !ADMIN_ROLE.equals(newRole.getRoleName())))){
            throw new AppException(ErrorCode.CANNOT_MODIFY_SELF);
        }

        userMapper.updateUser(user, request); // chỉ cập nhật các trường không null
        if(request.getEmail() != null){
            user.setEmail(blankToNull(request.getEmail()));
        }
        user.setRole(newRole);
        boolean passwordChanged = request.getPassword() != null && !request.getPassword().isEmpty();
        if(passwordChanged){
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        UserEntity saved = userRepository.save(user);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", snapshot(saved));
        if(passwordChanged){
            detail.put("passwordReset", true); // không bao giờ ghi mật khẩu vào nhật ký
        }
        auditLogService.log("UPDATE_USER", "Users", userId, detail);
        return userMapper.toUserResponse(saved);
    }

    /**
     * Xóa nhân viên. Tài khoản đã có lịch sử (nhật ký, phiếu...) thì chỉ khóa (xóa mềm, BACKLOG QT-01)
     * để không mất lịch sử giao dịch; tài khoản chưa dùng thì xóa hẳn.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserDeleteResponse deleteUser(Integer userId) {
        if(isCurrentUser(userId)){
            throw new AppException(ErrorCode.CANNOT_MODIFY_SELF);
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Boolean hasHistory = tx.execute(status -> {
            findUserById(userId);
            return auditLogRepository.existsByUserUserId(userId);
        });
        if(!Boolean.TRUE.equals(hasHistory)){
            try {
                tx.executeWithoutResult(status -> {
                    UserEntity user = findUserById(userId);
                    Map<String, Object> detail = snapshot(user);
                    userWarehouseRepository.deleteByUserId(userId);
                    pagePermissionRepository.deleteByUserId(userId);
                    userRepository.delete(user);
                    userRepository.flush(); // lộ lỗi khóa ngoại ngay tại đây nếu user đã tạo phiếu
                    auditLogService.log("DELETE_USER", "Users", userId, detail);
                });
                return new UserDeleteResponse(userId, true);
            } catch (DataIntegrityViolationException e) {
                // còn phiếu/chứng từ tham chiếu tới user: chuyển sang khóa tài khoản
            }
        }
        tx.executeWithoutResult(status -> {
            UserEntity user = findUserById(userId);
            user.setIsActive(false);
            userRepository.save(user);
            auditLogService.log("LOCK_USER", "Users", userId, snapshot(user));
        });
        return new UserDeleteResponse(userId, false);
    }

    // module khác (kho, phiếu...) dùng để lấy user
    public UserEntity findUserById(Integer userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private RoleEntity findRoleById(Integer roleId){
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }

    private static boolean isCurrentUser(Integer userId){
        return Objects.equals(SecurityUtils.currentUserId(), userId);
    }

    private static String blankToNull(String value){
        return value == null || value.isBlank() ? null : value.trim();
    }

    // thông tin ghi vào nhật ký (không có mật khẩu)
    private static Map<String, Object> snapshot(UserEntity user){
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", user.getUsername());
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("role", user.getRole() != null ? user.getRole().getRoleName() : null);
        data.put("isActive", user.getIsActive());
        return data;
    }
}
