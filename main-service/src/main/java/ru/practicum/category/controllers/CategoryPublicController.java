package ru.practicum.category.controllers;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
public class CategoryPublicController {
    private final CategoryService service;

    @GetMapping
    public List<CategoryDto> allCategories(@Min(0) @RequestParam(defaultValue = "0") Integer from,
                                           @Positive @RequestParam(defaultValue = "10") Integer size) {
        return service.findAll(from, size);
    }

    @GetMapping("/{catId}")
    public CategoryDto categoryById(@Positive @PathVariable Long catId) {
        return service.findById(catId);
    }
}
