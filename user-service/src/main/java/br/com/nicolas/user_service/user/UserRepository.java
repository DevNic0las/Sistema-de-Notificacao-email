package br.com.nicolas.user_service.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Boolean existsByEmail(String email);

  List<User> findByRole(UserRoles userRoles);

}
