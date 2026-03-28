package br.com.nicolas.user_service.user.dtos;

public record NotificationDTO(
  String senderName, String topic, String body

) {
}
