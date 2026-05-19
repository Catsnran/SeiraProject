package com.seira.dao.interfaces;

import com.seira.model.Category;

import java.util.List;

public interface ICategoryDAO {
    List<Category> findAll(int userId, String type);
    boolean add(Category c);
    boolean delete(int id);
}
