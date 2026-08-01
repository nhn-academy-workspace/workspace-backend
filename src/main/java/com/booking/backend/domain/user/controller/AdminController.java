package com.booking.backend.domain.user.controller;

import com.booking.backend.domain.user.dto.AllMemberResponse;
import com.booking.backend.domain.user.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('TA')")
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/teams")
    public ResponseEntity<List<AllMemberResponse>> getAllMember() {

        List<AllMemberResponse> res = adminService.getAll();

        return ResponseEntity.ok(res);
    }

}
