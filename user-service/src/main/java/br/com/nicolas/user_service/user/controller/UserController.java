package br.com.nicolas.user_service.user.controller;


import br.com.nicolas.user_service.user.dtos.UserRequestDTO;
import br.com.nicolas.user_service.user.dtos.UserResponseDTO;
import br.com.nicolas.user_service.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/user")
public class UserController {


  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }


  @PostMapping
  public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO userRequestDTO) {
    UserResponseDTO userResponseDTO = userService.createUser(userRequestDTO);
    URI uri = ServletUriComponentsBuilder
      .fromCurrentRequest()
      .path("/{id}")
      .buildAndExpand(userResponseDTO.id())
      .toUri();
    return ResponseEntity.created(uri).body(userResponseDTO);
  }


}
