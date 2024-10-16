package org.example.userauthenticationservice_sept2024.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Setter
@Getter
public class Session extends BaseModel {
  SessionState sessionState;

  String token;

  Date expiry;

  @ManyToOne
  User user;
}


//1      m
//user   session
//1       1
//
//
//1 : m