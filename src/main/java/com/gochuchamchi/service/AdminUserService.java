package com.gochuchamchi.service;

import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 회원 관리(권한 변경 / 계정 정지) 규칙. 화면에서 버튼을 숨겨도 요청은 직접 보낼 수 있으므로 여기서 다시 검사한다.
 *
 *   superadmin : 전부 가능 (admin 임명·해임, admin 정지)
 *   admin      : user / seller 만. admin 이상 계정은 열람만.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    public static final String ROLE_USER = "user";
    public static final String ROLE_SELLER = "seller";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_SUPERADMIN = "superadmin";

    /** admin 이 다룰 수 있는 등급. 밖의 계정은 admin 에게 읽기 전용 */
    private static final List<String> MANAGEABLE_BY_ADMIN = List.of(ROLE_USER, ROLE_SELLER);
    private static final List<String> ALL_ROLES = List.of(ROLE_USER, ROLE_SELLER, ROLE_ADMIN, ROLE_SUPERADMIN);

    public static final long MAX_SUSPEND_DAYS = 36500;    // 약 100년
    public static final long MAX_SUSPEND_UNIT = 100_000;  // 시/분/초 각각의 상한
    private static final long MAX_SUSPEND_SECONDS = (MAX_SUSPEND_DAYS + 1) * 86400L;

    private static final DateTimeFormatter UNTIL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    public static boolean isSuperAdmin(UserDto user) {
        return user != null && ROLE_SUPERADMIN.equals(user.getRole());
    }

    /** 대상 계정을 건드릴 수 있는지. 화면의 버튼 노출 판단에도 쓴다. */
    public static boolean canManage(UserDto actor, UserDto target) {
        if (actor == null || target == null) return false;
        if (actor.getId().equals(target.getId())) return false;      // 본인
        if (ROLE_SUPERADMIN.equals(target.getRole())) return false;  // superadmin 은 누구도 못 건드림
        return isSuperAdmin(actor) || MANAGEABLE_BY_ADMIN.contains(target.getRole());
    }

    // ===================== 권한 변경 =====================

    public void changeRole(UserDto actor, Long targetId, String newRole) {
        UserDto target = requireTarget(targetId);

        if (!ALL_ROLES.contains(newRole)) {
            throw new IllegalArgumentException("알 수 없는 권한 등급입니다.");
        }
        if (actor.getId().equals(target.getId())) {
            throw new IllegalArgumentException("본인 계정의 권한은 변경할 수 없습니다.");
        }
        if (ROLE_SUPERADMIN.equals(target.getRole())) {
            throw new IllegalArgumentException("슈퍼관리자 계정의 권한은 변경할 수 없습니다.");
        }
        if (!isSuperAdmin(actor)) {
            if (!MANAGEABLE_BY_ADMIN.contains(target.getRole())) {
                throw new IllegalArgumentException("관리자 계정의 권한은 변경할 수 없습니다. 슈퍼관리자에게 요청해주세요.");
            }
            if (!MANAGEABLE_BY_ADMIN.contains(newRole)) {
                throw new IllegalArgumentException("관리자 권한은 부여할 수 없습니다. 슈퍼관리자에게 요청해주세요.");
            }
        }
        if (newRole.equals(target.getRole())) {
            throw new IllegalArgumentException("이미 " + newRole + " 권한입니다.");
        }

        userMapper.updateRole(target.getId(), newRole);
        auditLogService.success("USER_ROLE_CHANGED", actor.getId(), actor.getUsername(),
                "USER", String.valueOf(target.getId()),
                "oldRole=" + target.getRole() + ";newRole=" + newRole);
    }

    // ===================== 계정 정지 =====================

    /** 계정 정지. permanent=true 면 기간 입력값을 무시하고 영구 정지 */
    public void suspend(UserDto actor, Long targetId,
                        long days, long hours, long minutes, long seconds,
                        boolean permanent) {
        UserDto target = requireTarget(targetId);

        if (actor.getId().equals(target.getId())) {
            throw new IllegalArgumentException("본인 계정은 정지할 수 없습니다.");
        }
        if (ROLE_SUPERADMIN.equals(target.getRole())) {
            throw new IllegalArgumentException("슈퍼관리자 계정은 정지할 수 없습니다.");
        }
        if (!isSuperAdmin(actor) && !MANAGEABLE_BY_ADMIN.contains(target.getRole())) {
            throw new IllegalArgumentException("관리자 계정은 정지할 수 없습니다. 슈퍼관리자에게 요청해주세요.");
        }

        if (permanent) {
            userMapper.suspend(target.getId(), null, true, actor.getUsername());
            auditLogService.success("USER_SUSPENDED", actor.getId(), actor.getUsername(),
                    "USER", String.valueOf(target.getId()), "permanent=true");
            return;
        }

        long total = toSeconds(days, hours, minutes, seconds);
        if (total <= 0) {
            throw new IllegalArgumentException("정지 기간을 입력해주세요. (영구 정지는 아래 체크박스를 사용하세요)");
        }
        if (total > MAX_SUSPEND_SECONDS) {
            throw new IllegalArgumentException(
                "정지 기간이 너무 깁니다. 합쳐서 " + MAX_SUSPEND_DAYS + "일(약 100년)을 넘을 수 없습니다.");
        }

        userMapper.suspend(target.getId(), LocalDateTime.now().plusSeconds(total), false, actor.getUsername());
        auditLogService.success("USER_SUSPENDED", actor.getId(), actor.getUsername(),
                "USER", String.valueOf(target.getId()), "durationSeconds=" + total);
    }

    public void unsuspend(UserDto actor, Long targetId) {
        UserDto target = requireTarget(targetId);

        if (ROLE_SUPERADMIN.equals(target.getRole())) {
            throw new IllegalArgumentException("슈퍼관리자 계정은 정지 대상이 아닙니다.");
        }
        if (!isSuperAdmin(actor) && !MANAGEABLE_BY_ADMIN.contains(target.getRole())) {
            throw new IllegalArgumentException("관리자 계정의 정지는 해제할 수 없습니다. 슈퍼관리자에게 요청해주세요.");
        }

        userMapper.unsuspend(target.getId());
        auditLogService.success("USER_UNSUSPENDED", actor.getId(), actor.getUsername(),
                "USER", String.valueOf(target.getId()), null);
    }

    /** 정지된 계정이 로그인을 시도했을 때 보여줄 안내 문구 */
    public String suspensionMessage(UserDto user) {
        if (user.isSuspendedPermanent()) {
            return "사용이 정지된 계정입니다. 영구적으로 사용이 불가하며 "
                 + "자세한 문의는 공지사항을 참고하시기 바랍니다. 이용의 불편을 드려 죄송합니다.";
        }
        return "사용이 정지된 계정입니다. "
             + user.getSuspendedUntil().format(UNTIL_FORMAT) + "까지"
             + " (남은 기간 " + remainingText(user.getSuspendedUntil()) + ") 사용이 불가하며 "
             + "자세한 문의는 공지사항을 참고하시기 바랍니다. 이용의 불편을 드려 죄송합니다.";
    }

    /** "3일 2시간 10분" 형태의 남은 기간 */
    private String remainingText(LocalDateTime until) {
        long secs = Math.max(0, Duration.between(LocalDateTime.now(), until).getSeconds());
        long d = secs / 86400, h = (secs % 86400) / 3600, m = (secs % 3600) / 60, s = secs % 60;

        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("일 ");
        if (h > 0) sb.append(h).append("시간 ");
        if (m > 0) sb.append(m).append("분 ");
        if (sb.length() == 0) sb.append(s).append("초 ");
        return sb.toString().trim();
    }

    private long toSeconds(long days, long hours, long minutes, long seconds) {
        if (days < 0 || hours < 0 || minutes < 0 || seconds < 0) {
            throw new IllegalArgumentException("정지 기간은 0 이상으로 입력해주세요.");
        }
        // 조용히 자르지 않고 알려준다. 여기서 걸러야 아래 덧셈이 넘치지 않음
        if (days > MAX_SUSPEND_DAYS) {
            throw new IllegalArgumentException(
                "정지 기간(일)은 최대 " + MAX_SUSPEND_DAYS + "일(약 100년)까지 입력할 수 있습니다.");
        }
        if (hours > MAX_SUSPEND_UNIT || minutes > MAX_SUSPEND_UNIT || seconds > MAX_SUSPEND_UNIT) {
            throw new IllegalArgumentException(
                "시간 · 분 · 초는 각각 " + MAX_SUSPEND_UNIT + " 이하로 입력해주세요.");
        }
        return days * 86400 + hours * 3600 + minutes * 60 + seconds;
    }

    private UserDto requireTarget(Long targetId) {
        UserDto target = userMapper.findById(targetId);
        if (target == null) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }
        return target;
    }
}
