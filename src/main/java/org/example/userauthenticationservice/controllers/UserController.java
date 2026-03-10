package org.example.userauthenticationservice.controllers;

import org.example.userauthenticationservice.dtos.UpdateProfileRequestDto;
import org.example.userauthenticationservice.dtos.UserDto;
import org.example.userauthenticationservice.models.User;
import org.example.userauthenticationservice.services.AuthService;
import org.example.userauthenticationservice.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    /**
     * FR-1.3: Get user profile details.
     * Called by ProductCatalogService via Eureka @LoadBalanced RestTemplate.
     */
    @GetMapping("/users/{id}")
    public UserDto getUserDetails(@PathVariable Long id) {
        User user = userService.getUserDetails(id);
        UserDto userDto = new UserDto();
        userDto.setEmail(user.getEmail());
        System.out.println(user.getEmail());
        return userDto;
    }

    /**
     * FR-1.3: Update user profile (email or password).
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<UserDto> updateProfile(@PathVariable Long id,
                                                  @RequestBody UpdateProfileRequestDto request) {
        try {
            User updated = authService.updateProfile(id, request.getEmail(), request.getPassword());
            UserDto userDto = new UserDto();
            userDto.setEmail(updated.getEmail());
            return new ResponseEntity<>(userDto, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
