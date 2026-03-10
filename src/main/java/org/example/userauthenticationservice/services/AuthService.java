package org.example.userauthenticationservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.antlr.v4.runtime.misc.Pair;
import org.example.userauthenticationservice.clients.KafkaProducerClient;
import org.example.userauthenticationservice.dtos.EmailDto;
import org.example.userauthenticationservice.exceptions.UserAlreadyExistsException;
import org.example.userauthenticationservice.exceptions.UserNotFoundException;
import org.example.userauthenticationservice.exceptions.WrongPasswordException;
import org.example.userauthenticationservice.models.Session;
import org.example.userauthenticationservice.models.SessionState;
import org.example.userauthenticationservice.models.User;
import org.example.userauthenticationservice.repositories.SessionRepo;
import org.example.userauthenticationservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import javax.crypto.SecretKey;
import java.util.Optional;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bcryptPasswordEncoder;

    @Autowired
    private SessionRepo sessionRepo;

    @Autowired
    private SecretKey secretKey;

    @Autowired
    private KafkaProducerClient kafkaProducerClient;

    @Autowired
    private ObjectMapper objectMapper;

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

        //sending email logic
        try {
            EmailDto emailDto = new EmailDto();
            emailDto.setTo(email);
            emailDto.setFrom("anuragbatch@gmail.com");
            emailDto.setSubject("Welcome to Scaler !!");
            emailDto.setBody("Hope you have great stay.");
            kafkaProducerClient.sendMessage("signup", objectMapper.writeValueAsString(emailDto));
        }catch (JsonProcessingException exception) {
            throw new RuntimeException(exception.getMessage());
        }


        return true;
    }

    public Pair<Boolean,String> login(String email, String password) throws UserNotFoundException, WrongPasswordException {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new UserNotFoundException("User with email: " + email + " not found.");
        }
        //boolean matches = password.equals(userOptional.get().getPassword());
        boolean matches = bcryptPasswordEncoder.matches(password,userOptional.get().getPassword());

        //check current time stamp and compare with session timestamp and then mark entry as
        //active or expired

        //JWT Generation
//        String message = "{\n" +
//                "   \"email\": \"anurag@gmail.com\",\n" +
//                "   \"roles\": [\n" +
//                "      \"instructor\",\n" +
//                "      \"ta\"\n" +
//                "   ],\n" +
//                "   \"expirationDate\": \"2ndApril2025\"\n" +
//                "}";

       // byte[] content = message.getBytes(StandardCharsets.UTF_8);

        Map<String,Object> claims  = new HashMap<>();
        Long currentTimeInMillis = System.currentTimeMillis();
        claims.put("iat",currentTimeInMillis);
        claims.put("exp",currentTimeInMillis+864000);
        claims.put("user_id",userOptional.get().getId());
        claims.put("issuer","scaler");

        String token  = Jwts.builder().claims(claims).signWith(secretKey).compact();

        Session session = new Session();
        session.setToken(token);
        session.setSessionState(SessionState.ACTIVE);
        session.setUser(userOptional.get());
        sessionRepo.save(session);

        if (matches) {
            return new Pair<Boolean,String>(true,token);
        } else {
            throw new WrongPasswordException("Wrong password.");
        }
    }


    //xyxyxyxyyx.hdiwhdiwhi.budiwhiowheori

    public Boolean validateToken(Long userId, String token) {
       Optional<Session> optionalSession = sessionRepo.findByTokenAndUser_Id(token,userId);

       if(optionalSession.isEmpty()) {
           System.out.println("Token or userId not found");
           return false;
       }

        JwtParser jwtParser = Jwts.parser().verifyWith(secretKey).build();
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();

        Long expiry = (Long)claims.get("exp");
        Long currentTimeStamp = System.currentTimeMillis();

        if(currentTimeStamp > expiry) {
            System.out.println(expiry);
            System.out.println(currentTimeStamp);
            System.out.println("Token is expired");

            //Marking session entry as expired
            optionalSession.get().setSessionState(SessionState.EXPIRED);
            sessionRepo.save(optionalSession.get());
            return false;
        }

        return true;
    }

    /**
     * Logout: marks the session as EXPIRED in MySQL (FR-6.2).
     * Subsequent validateToken calls will return false for this token.
     */
    public boolean logout(Long userId, String token) {
        Optional<Session> optionalSession = sessionRepo.findByTokenAndUser_Id(token, userId);
        if (optionalSession.isEmpty()) {
            return false;
        }
        Session session = optionalSession.get();
        session.setSessionState(SessionState.EXPIRED);
        sessionRepo.save(session);
        return true;
    }

    /**
     * Update user profile details (FR-1.3).
     * Allows users to update their email. Password change triggers BCrypt re-hash.
     */
    public User updateProfile(Long userId, String newEmail, String newPassword) throws UserNotFoundException {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        User user = optionalUser.get();
        if (newEmail != null && !newEmail.isBlank()) {
            user.setEmail(newEmail);
        }
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(bcryptPasswordEncoder.encode(newPassword));
        }
        return userRepository.save(user);
    }

    /**
     * Password reset via email link (FR-1.4).
     * Generates a reset token, sends it to user's email via Kafka.
     */
    public boolean initiatePasswordReset(String email) throws UserNotFoundException {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("No user found with email: " + email);
        }
        // Generate a short-lived reset token (valid 15 min)
        String resetToken = Jwts.builder()
                .claim("email", email)
                .claim("purpose", "password_reset")
                .signWith(secretKey)
                .compact();

        // Publish reset token to Kafka — EmailService sends the reset link email
        EmailDto emailDto = new EmailDto();
        emailDto.setTo(email);
        emailDto.setFrom("noreply@ecommerce.com");
        emailDto.setSubject("Password Reset Request");
        emailDto.setBody("Click the following link to reset your password: " +
                "https://ecommerce.com/reset-password?token=" + resetToken);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            kafkaProducerClient.sendMessage("password-reset", objectMapper.writeValueAsString(emailDto));
        } catch (JsonProcessingException e) {
            System.err.println("Failed to publish password reset event: " + e.getMessage());
        }
        return true;
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

