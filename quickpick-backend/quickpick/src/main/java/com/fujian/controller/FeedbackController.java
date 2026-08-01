package com.fujian.controller;

import com.fujian.common.Result;
import com.fujian.mapper.FeedbackMapper;
import com.fujian.pojo.Feedback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/client/feedbacks")
public class FeedbackController {

    @Autowired
    private FeedbackMapper feedbackMapper;

    @PostMapping
    public Result<String> submitFeedback(@RequestBody Map<String, String> request) {
        Long userId;
        try {
            userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            return Result.error("未登录或登录已过期");
        }

        String content = request.get("content");
        String contact = request.get("contact");

        if (content == null || content.trim().isEmpty()) {
            return Result.error("反馈内容不能为空");
        }

        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setContent(content);
        feedback.setContact(contact);
        feedback.setCreateTime(LocalDateTime.now());

        feedbackMapper.insert(feedback);

        return Result.success("反馈提交成功");
    }
}
