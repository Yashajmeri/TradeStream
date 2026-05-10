package com.example.TradeStream.userService.controller;

import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.repository.UserRepository;
import com.example.TradeStream.userService.security.JWT.JwtUtils;
import com.example.TradeStream.userService.security.service.UserDetailsImpl;
import com.example.TradeStream.userService.security.response.MessageResponse;
import com.example.TradeStream.userService.security.response.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Authentication", description = "User registration and JWT sign-in")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Operation(summary = "Register a new user",
            responses = {
                @ApiResponse(responseCode = "200", description = "User registered successfully"),
                @ApiResponse(responseCode = "400", description = "Username or email already taken")
            })
    @PostMapping("/signup")
    public ResponseEntity<?> register(@Validated @RequestBody User user) {

     if(userRepository.existsByUsername(user.getUsername())) {
         return ResponseEntity.badRequest()
                 .body(new MessageResponse("Error : Username "+user.getUsername() +"Already Exist !!"));
     }
     if(userRepository.existsByEmail(user.getEmail())) {
         return ResponseEntity.badRequest()
                 .body(new MessageResponse("Error: Email "+ user.getEmail()+"already Exist !"));

     }
        User newUser = new User();
        newUser.setEmail(user.getEmail());
        newUser.setUsername(user.getUsername());
        newUser.setFullName(user.getFullName());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
          User savedUser = userRepository.save(newUser);
        return ResponseEntity.ok(new MessageResponse("User registered successfully with username: " + savedUser.getUsername()));

    }
    @Operation(summary = "Sign in and receive a JWT token",
            responses = {
                @ApiResponse(responseCode = "200", description = "JWT token returned"),
                @ApiResponse(responseCode = "404", description = "Bad credentials")
            })
    @PostMapping("/signin")
public ResponseEntity<?> AuthenticateUSer(@RequestBody User user) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            user.getPassword()));

        }catch (AuthenticationException e) {
            Map<String,Object> map  = new HashMap<>();
            map.put("message","Bad credentials");
            map.put("status",false);
            return new ResponseEntity<>(map, HttpStatus.NOT_FOUND);

        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generateJwtToken(userDetails);
        UserInfoResponse userInfoResponse = new UserInfoResponse(jwt,userDetails.getUsername(),userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),userDetails.getId());
        userInfoResponse.setMessage("Login successful : Welcome "+userDetails.getUsername());
        return ResponseEntity.ok(userInfoResponse);
    }

    @Operation(summary = "Sign out and clear the security context",
            responses = {
                @ApiResponse(responseCode = "200", description = "Signed out successfully")
            })
    @PostMapping("/signout")
    public ResponseEntity<?> signout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponse("You have been successfully signed out."));
    }
}
