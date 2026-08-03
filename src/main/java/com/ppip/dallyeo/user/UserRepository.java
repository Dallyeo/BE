package com.ppip.dallyeo.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 사용자 영속성. 재로그인 매칭 = (provider, providerUserId) 복합 유니크(Q3=A).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUserId(Provider provider, String providerUserId);
}
