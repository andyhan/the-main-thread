package com.example.architecture.repository;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository {

    private final List<String> users = new ArrayList<>();

    public void addUser(String user) {
        users.add(user);
    }

    public List<String> getAllUsers() {
        return new ArrayList<>(users);
    }
}