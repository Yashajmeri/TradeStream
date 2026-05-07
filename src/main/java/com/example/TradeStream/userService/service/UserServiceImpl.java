package com.example.TradeStream.userService.service;


import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.repository.UserRepository;
import com.example.TradeStream.walletService.exception.APIException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
private final UserRepository userRepository;
    @Transactional(readOnly = true)
    @Override
    public User getUserByUserName(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new APIException("User not found with username: " + username));
    }
}
