package com.aicopilot.api.service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aicopilot.api.common.exception.BizException;
import com.aicopilot.api.common.result.ResultCode;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件上传服务 - 基于 MinIO 对象存储
 * 支持图片（jpg/png/gif/webp）和文档（pdf/md/txt/docx 等）上传
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket}")
    private String bucket;

    /** 最大文件大小：10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    /**
     * 上传文件并返回可访问的 URL
     *
     * @param file 用户上传文件
     * @param type "image" or "file"
     * @return 公开访问的文件 URL
     */
    public String upload(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.FAIL.getCode(), "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ResultCode.FAIL.getCode(), "文件大小不能超过 10MB");
        }

        try {
            ensureBucketExists();

            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String suffix = "";
            int dot = original.lastIndexOf('.');
            if (dot > -1) {
                suffix = original.substring(dot);
            }
            String dir = "image".equals(type) ? "images" : "files";
            String objectName = String.format("%s/%s/%s%s",
                    dir, LocalDate.now(), UUID.randomUUID().toString().replace("-", ""), suffix);

            try (InputStream is = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .stream(is, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
            }

            return String.format("%s/%s/%s", endpoint, bucket, objectName);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BizException(ResultCode.FAIL.getCode(), "文件上传失败: " + e.getMessage());
        }
    }

    /** 确保 bucket 存在并设置为公开读 */
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            // 设置匿名读取权限，便于 LLM 多模态访问
            String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::"
                    + bucket + "/*\"]}]}";
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
        }
    }
}