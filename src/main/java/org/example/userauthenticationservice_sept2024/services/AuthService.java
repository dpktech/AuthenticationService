package org.example.userauthenticationservice_sept2024.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.MacAlgorithm;
import org.antlr.v4.runtime.misc.Pair;
import org.example.userauthenticationservice_sept2024.exceptions.UserAlreadyExistsException;
import org.example.userauthenticationservice_sept2024.exceptions.UserNotFoundException;
import org.example.userauthenticationservice_sept2024.exceptions.WrongPasswordException;
import org.example.userauthenticationservice_sept2024.models.User;
import org.example.userauthenticationservice_sept2024.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bcryptPasswordEncoder;

//    public AuthService(UserRepository userRepository,BCryptPasswordEncoder bcryptPasswordEncoder) {
//        this.userRepository = userRepository;
//        this.bcryptPasswordEncoder = bcryptPasswordEncoder;
//    }

    public boolean signUp(String email, String password) throws UserAlreadyExistsException {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("User with email: " + email + " already exists");
        }
        User user = new User();
        user.setEmail(email);
        String hashedPassword = bcryptPasswordEncoder.encode(password);
        //user.setPassword(password);
        user.setPassword(hashedPassword);
        userRepository.save(user);
        return true;
    }

    public Pair<Boolean,String> login(String email, String password) throws UserNotFoundException, WrongPasswordException {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new UserNotFoundException("User with email: " + email + " not found.");
        }
        //boolean matches = password.equals(userOptional.get().getPassword());
        boolean matches = bcryptPasswordEncoder.matches(password,userOptional.get().getPassword());


        //JWT Generation
//        String message = "{\n" +
//                "   \"email\": \"anurag@gmail.com\",\n" +
//                "   \"roles\": [\n" +
//                "      \"instructor\",\n" +
//                "      \"ta\"\n" +
//                "   ],\n" +
//                "   \"expirationDate\": \"2ndApril2025\"\n" +
//                "}";

        MacAlgorithm algorithm = Jwts.SIG.HS256;
        SecretKey secretKey = algorithm.key().build();
       // byte[] content = message.getBytes(StandardCharsets.UTF_8);

        Map<String,Object> claims  = new HashMap<>();
        Long currentTimeInMillis = System.currentTimeMillis();
        claims.put("iat",currentTimeInMillis);
        claims.put("exp",currentTimeInMillis+864000);
        claims.put("user_id",userOptional.get().getId());
        claims.put("issuer","scaler");

        String token  = Jwts.builder().claims(claims).signWith(secretKey).compact();

        if (matches) {
            return new Pair<Boolean,String>(true,token);
        } else {
            throw new WrongPasswordException("Wrong password.");
        }
    }
}






//stored token somewhere
//
//validateToken(inputtoken)
//
//    inputtoken == token_persisted ->(valid token)
//    token is expired or not  ?
//         -> decode token (using same secretkey) and get payload
//             -> from payload -> get expiry and check if it's expired or not

