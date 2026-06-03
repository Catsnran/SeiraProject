package com.seira.dao.interfaces;

import com.seira.models.Category;

import java.util.List;

public interface ICategoryDAO {
    List<Category> findAll(int userId, String type);
    List<Category> findAllByUser(int userId);
    boolean add(Category c);
    boolean update(Category c);
    boolean delete(int id);
}