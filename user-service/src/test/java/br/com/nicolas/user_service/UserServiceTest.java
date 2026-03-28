package br.com.nicolas.user_service;


import br.com.nicolas.user_service.exceptions.EmailAlreadyExistsException;
import br.com.nicolas.user_service.user.User;
import br.com.nicolas.user_service.user.UserRepository;
import br.com.nicolas.user_service.user.UserRoles;
import br.com.nicolas.user_service.user.dtos.UserRequestDTO;
import br.com.nicolas.user_service.user.dtos.UserResponseDTO;
import br.com.nicolas.user_service.user.producer.UserProducer;
import br.com.nicolas.user_service.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
  @Mock
  private UserRepository userRepository;

  @Mock
  private UserProducer userProducer;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  private UserRequestDTO validRequest;

  @BeforeEach
  void setUp() {
    validRequest = new UserRequestDTO("João", "joao@email.com", "Senha@123", UserRoles.CLIENT);
  }

  @Test
  public void shouldThrowExceptionWhenEmailAlreadyExists(){
    //arrange
    when(userRepository.existsByEmail(validRequest.email())).thenReturn(true);

    assertThrows(EmailAlreadyExistsException.class, ()->{
      userService.createUser(validRequest);
    });

    verify(userRepository, never()).save(any());
    verify(userProducer, never()).userCreated(any());
  }

  @Test
  public void shouldCreateUser(){
    // Arrange
    when(userRepository.existsByEmail(validRequest.email())).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("encoded-password");

    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setEmail(validRequest.email());
    savedUser.setName(validRequest.name());

    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    UserResponseDTO response = userService.createUser(validRequest);

    // Assert
    assertTrue(response.email().equals(validRequest.email()));
    assertTrue(response.name().equals(validRequest.name()));

    verify(userRepository).save(any(User.class));
    verify(userProducer).userCreated(any());
  }




}
