package br.com.nicolas.user_service.exceptions;

public record ErroResponse(int status,
                           String message,
                           long timestamp) {
}
