package br.com.nicolas.user_service.user.controller;


import br.com.nicolas.user_service.user.dtos.NotificationDTO;
import br.com.nicolas.user_service.user.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminController {

  private final AdminService adminService;


  public AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @PostMapping("/staff")
  public ResponseEntity<Void> sendEmailForStaff(@RequestBody NotificationDTO notificationDTO) {
    adminService.sendEmailToStaff(notificationDTO);
    return ResponseEntity.accepted().build();
  }


}
