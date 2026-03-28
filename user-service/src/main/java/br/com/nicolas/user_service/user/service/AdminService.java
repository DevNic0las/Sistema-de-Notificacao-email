package br.com.nicolas.user_service.user.service;


import br.com.nicolas.user_service.user.User;
import br.com.nicolas.user_service.user.UserRepository;
import br.com.nicolas.user_service.user.UserRoles;
import br.com.nicolas.user_service.user.dtos.EmailDTO;
import br.com.nicolas.user_service.user.dtos.NotificationDTO;
import br.com.nicolas.user_service.user.producer.AdminProducer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

  private final AdminProducer adminProducer;
  private final UserRepository userRepository;

  public AdminService(AdminProducer adminProducer, UserRepository userRepository) {
    this.adminProducer = adminProducer;
    this.userRepository = userRepository;
  }

  public void sendEmailToStaff(NotificationDTO notificationDTO) {
    List<User> listStaff = userRepository.findByRole(UserRoles.STAFF);
    for (User staff : listStaff) {
      EmailDTO emailDTO = new EmailDTO(
        staff.getEmail(),
        notificationDTO.topic(),
        "Olá " + staff.getName() + ",\n\n" + notificationDTO.body() + "\n\nAtenciosamente,\n"
          + notificationDTO.senderName()
      );
      adminProducer.sendToStaff(emailDTO);
    }

  }


}
