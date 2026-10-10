package com.atm.auth.service;

import com.atm.auth.entity.Card;
import com.atm.auth.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security {@link UserDetailsService} backed by the {@code cards} table.
 *
 * <p>The "username" is the card number.</p>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CardRepository cardRepository;

    @Override
    public UserDetails loadUserByUsername(String cardNumber) throws UsernameNotFoundException {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new UsernameNotFoundException("Card not found: " + cardNumber));

        boolean enabled = card.getStatus() == Card.CardStatus.ACTIVE;
        boolean accountNonLocked = card.getStatus() != Card.CardStatus.BLOCKED;

        // Note: Spring Security's UserBuilder API uses the negative forms
        // (disabled / accountLocked / accountExpired / credentialsExpired).
        return User.builder()
                .username(card.getCardNumber())
                .password(card.getPinHash())
                .disabled(!enabled)
                .accountLocked(!accountNonLocked)
                .accountExpired(false)
                .credentialsExpired(false)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CARDHOLDER")))
                .build();
    }
}
