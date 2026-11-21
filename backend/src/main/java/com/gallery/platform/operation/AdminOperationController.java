package com.gallery.platform.operation;

import com.gallery.platform.operation.dto.OperationDtos;
import com.gallery.platform.security.AppPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/operations")
public class AdminOperationController {
    private final OperationService operationService;

    public AdminOperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @PostMapping
    public ResponseEntity<OperationDtos.OperationView> create(@AuthenticationPrincipal AppPrincipal principal,
                                                              @Valid @RequestBody OperationDtos.CreateOperationRequest req,
                                                              HttpServletRequest request) {
        return ResponseEntity.ok(operationService.create(principal, req, request));
    }

    @GetMapping
    public Page<OperationDtos.OperationView> list(@AuthenticationPrincipal AppPrincipal principal,
                                                  @RequestParam(required = false) Long deviceId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return operationService.listForAdmin(principal, deviceId, page, size);
    }

    @GetMapping("/{id}")
    public OperationDtos.OperationView get(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
        return operationService.getForAdmin(principal, id);
    }
}
