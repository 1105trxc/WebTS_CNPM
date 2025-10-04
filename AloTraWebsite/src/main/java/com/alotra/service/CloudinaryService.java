package com.alotra.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            // Prefer HTTPS URL to avoid mixed-content issues
            Object secureUrl = result.get("secure_url");
            Object url = result.get("url");
            return (secureUrl != null ? secureUrl.toString() : (url != null ? url.toString() : null));
        } catch (IOException e) {
            throw new RuntimeException("Lỗi tải ảnh lên Cloudinary", e);
        }
    }
}