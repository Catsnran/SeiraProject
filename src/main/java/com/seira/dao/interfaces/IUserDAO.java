package com.seira.dao.interfaces;

import com.seira.models.User;

public interface IUserDAO {
    boolean register(String username, String email, String password);
    User login(String email, String password);
    boolean emailExists(String email);
}
