package com.fujian.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class CosService {
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");


    @Value("${tencent.cos.secret-id}")
    private String secretId;

    @Value("${tencent.cos.secret-key}")
    private String secretKey;

    @Value("${tencent.cos.region}")
    private String regionName;

    @Value("${tencent.cos.bucket}")
    private String bucketName;

    @Value("${tencent.cos.base-url}")
    private String baseUrl;

    private COSClient cosClient;

    @PostConstruct
    public void init() {
        // 1 初始化用户身份信息 (secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域
        Region region = new Region(regionName);
        ClientConfig clientConfig = new ClientConfig(region);
        // 这里建议设置为 https
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成 cos 客户端
        cosClient = new COSClient(cred, clientConfig);
    }

    public String upload(MultipartFile file, String folder) {
        try {
            String key = buildLegacyObjectKey(file, folder);

            // 获取输入流
            InputStream inputStream = file.getInputStream();
            
            // 设置元数据
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());

            // 上传
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, objectMetadata);
            cosClient.putObject(putObjectRequest);

            // 返回完整访问路径
            return baseUrl + "/" + key;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    public String upload(MultipartFile file, String folder, Long shopId, String filePrefix) {
        try {
            String key = buildShopScopedObjectKey(file, folder, shopId, filePrefix);

            // 获取输入流
            InputStream inputStream = file.getInputStream();
            
            // 设置元数据
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());

            // 上传
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, objectMetadata);
            cosClient.putObject(putObjectRequest);

            // 返回完整访问路径
            return baseUrl + "/" + key;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    private String buildLegacyObjectKey(MultipartFile file, String folder) {
        String suffix = extractFileSuffix(file);
        String fileName = UUID.randomUUID() + suffix;
        return folder != null && !folder.isEmpty() ? folder + "/" + fileName : fileName;
    }

    private String buildShopScopedObjectKey(MultipartFile file, String folder, Long shopId, String filePrefix) {
        String suffix = extractFileSuffix(file);
        String normalizedFolder = normalizeFolder(folder);
        String normalizedPrefix = normalizePrefix(filePrefix);
        String timePart = LocalDateTime.now().format(FILE_TIME_FORMATTER);
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String fileName = normalizedPrefix + "_" + timePart + "_" + randomPart + suffix;
        return normalizedFolder + "/" + shopId + "/" + fileName;
    }

    private String extractFileSuffix(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
                return originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
            }
        }

        String contentType = file.getContentType();
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        if ("image/gif".equalsIgnoreCase(contentType)) {
            return ".gif";
        }
        return ".jpg";
    }

    private String normalizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            throw new IllegalArgumentException("上传目录不能为空");
        }
        return folder.trim().replace("\\", "/").replaceAll("/+$", "");
    }

    private String normalizePrefix(String filePrefix) {
        if (filePrefix == null || filePrefix.isBlank()) {
            return "image";
        }
        return filePrefix.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }

    // 保持原有方法兼容性，默认上传到根目录
    public String upload(MultipartFile file) {
        return upload(file, "");
    }
}
