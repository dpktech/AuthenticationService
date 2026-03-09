package org.example.userauthenticationservice.services;

import org.example.userauthenticationservice.models.User;
import org.example.userauthenticationservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User getUserDetails(Long id) {
        return userRepository.findById(id).get();
    }
}
