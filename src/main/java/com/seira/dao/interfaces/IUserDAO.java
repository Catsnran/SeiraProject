package com.seira.dao.interfaces;

import com.seira.models.User;

public interface IUserDAO {
    boolean register(String username, String email, String password);
    User login(String email, String password);
    boolean emailExists(String email);
    User findByEmail(String email);

    // update user ? 1 : 0
    int updateProfile(int userId, String username, String email, String newPassword, String profilePhoto, String currency);

    // select by id
    User findById(int userId);
}
