package com.example.TradeStream.userService;

import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.repository.UserRepository;
import com.example.TradeStream.userService.service.UserServiceImpl;
import com.example.TradeStream.walletService.exception.APIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setPassword("encoded-password");
    }

    @Test
    void getUserByUserName_whenFound_returnsUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        User result = userService.getUserByUserName("testuser");

        assertThat(result).isEqualTo(user);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserByUserName_whenNotFound_throwsAPIException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByUserName("unknown"))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("unknown");
    }
}
