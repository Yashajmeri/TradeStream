package com.example.TradeStream.userService.security.response;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class UserInfoResponse {

    private Long id;
    private String Message;
    private String jwtToken;
    private String username;
    private List<String> roles;
    public UserInfoResponse(String jwtToken, String username, List<String> roles , Long id) {
        this.jwtToken = jwtToken;
        this.username = username;
        this.roles = roles;
        this.id = id;
    }

}