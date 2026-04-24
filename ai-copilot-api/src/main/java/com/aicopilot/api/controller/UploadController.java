package com.aicopilot.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aicopilot.api.common.result.R;
import com.aicopilot.api.service.FileUploadService;

import lombok.RequiredArgsConstructor;

/**
 * 文件上传接口
 * - POST /uploads?type=image 上传图片（多模态）
 * - POST /uploads?type=file 上传普通文件（参考文档）
 */
@RestController
@RequiredArgsConstructor
public class UploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/uploads")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "file") String type) {
        String url = fileUploadService.upload(file, type);
        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        data.put("name", file.getOriginalFilename());
        data.put("size", file.getSize());
        data.put("type", type);
        return R.ok(data);
    }
}