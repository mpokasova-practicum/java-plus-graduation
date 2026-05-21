package ru.practicum.category.service;

import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    List<CategoryDto> findAll(Integer from, Integer size);

    CategoryDto findById(Long catId);

    CategoryDto createById(NewCategoryDto dto);

    CategoryDto updateById(Long catId, NewCategoryDto dto);

    void deleteById(Long catId);
}
