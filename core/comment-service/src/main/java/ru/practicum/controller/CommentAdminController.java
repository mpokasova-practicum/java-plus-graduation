package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.AdminUpdateCommentStatusDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/comments")
public class CommentAdminController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> adminPendigCommentList(@RequestParam(required = false) List<Long> users) {
        return commentService.adminPendigCommentList(users);
    }

    @PatchMapping("/{commentId}")
    public CommentDto adminUpdateCommentStatus(@PathVariable("commentId") Long commentId,
                                               @Valid @RequestBody AdminUpdateCommentStatusDto dto) {
        return commentService.adminUpdateCommentStatus(commentId, dto);
    }

    @DeleteMapping("/{commentId}")
    public void adminDeleteComment(@PathVariable Long commentId) {
        commentService.adminDeleteComment(commentId);
    }
}
