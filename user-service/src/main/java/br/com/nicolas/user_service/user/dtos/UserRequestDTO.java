package br.com.nicolas.user_service.user.dtos;

import br.com.nicolas.user_service.user.UserRoles;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserRequestDTO(

  @NotBlank(message = "O nome é obrigatório")
  String name,
  @NotBlank(message = "O email é obrigatório")
  @Email(message = "Digite um email válido")
  String email,
  @NotBlank(message = "A senha é obrigatória")
  @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$", message = "A senha deve ter pelo menos 8 caracteres, incluindo uma letra maiúscula, uma minúscula, um número e um caractere especial")
  String password,
  @NotNull(message = "O papel é obrigatório")
  UserRoles role
) {
}
