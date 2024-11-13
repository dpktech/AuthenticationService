package org.example.userauthenticationservice_sept2024.controllers;

import jakarta.persistence.Access;
import org.example.userauthenticationservice_sept2024.dtos.UserDto;
import org.example.userauthenticationservice_sept2024.models.User;
import org.example.userauthenticationservice_sept2024.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/users/{id}")
    public UserDto getUserDetails(@PathVariable Long id) {
        User user = userService.getUserDetails(id);

        UserDto userDto = new UserDto();
        userDto.setEmail(user.getEmail());
        System.out.println(user.getEmail());
        return userDto;
    }
}
