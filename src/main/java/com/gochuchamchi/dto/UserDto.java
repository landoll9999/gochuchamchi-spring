package com.gochuchamchi.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String birthdate;
    private String gender;
    private String nationality;
    private String address;
    private String role;        // user, seller, admin, superadmin
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== 계정 사용 정지 =====
    private LocalDateTime suspendedUntil;   // 정지 해제 시각 (영구 정지면 무시)
    private boolean suspendedPermanent;     // 영구 정지 여부
    private LocalDateTime suspendedAt;      // 정지시킨 시각
    private String suspendedBy;             // 정지시킨 관리자 아이디

    /** 지금 로그인이 차단되어야 하는 계정인지. 기간이 지난 정지는 자동으로 풀린 것으로 본다. */
    public boolean isSuspended() {
        if (suspendedPermanent) return true;
        return suspendedUntil != null && suspendedUntil.isAfter(LocalDateTime.now());
    }
}
