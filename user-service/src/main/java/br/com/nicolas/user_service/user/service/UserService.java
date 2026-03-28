package br.com.nicolas.user_service.user.service;


import br.com.nicolas.user_service.exceptions.EmailAlreadyExistsException;
import br.com.nicolas.user_service.user.User;
import br.com.nicolas.user_service.user.UserRepository;
import br.com.nicolas.user_service.user.dtos.UserRequestDTO;
import br.com.nicolas.user_service.user.dtos.UserResponseDTO;
import br.com.nicolas.user_service.user.producer.UserProducer;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final UserProducer userProducer;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public UserService(UserRepository userRepository, UserProducer userProducer, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.userProducer = userProducer;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public UserResponseDTO createUser(UserRequestDTO userDTO) {
    if (userRepository.existsByEmail(userDTO.email())) {
      throw new EmailAlreadyExistsException("Email já cadastrado");
    }

    User user = new User();
    user.setName(userDTO.name());
    user.setEmail(userDTO.email());
    user.setPassword(passwordEncoder.encode(userDTO.password()));
    user.setRole(userDTO.role());
    User userSaved = userRepository.save(user);
    UserResponseDTO response = new UserResponseDTO(userSaved.getId(), userSaved.getEmail(), userSaved.getName());
    userProducer.userCreated(response);
    return response;
  }


}
