package br.com.nicolas.user_service.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailDTO(
  @NotBlank(message = "Destinatário é obrigatório")
  @Email(message = "Formato de email inválido")
  String to,
  @NotBlank(message = "Assunto é obrigatório")
  String subject,
  @NotBlank(message = "Corpo é obrigatório")

  String body


) {
}
