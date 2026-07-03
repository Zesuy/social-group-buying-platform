package com.example.groupshop.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Component
@ConfigurationProperties(prefix = "groupshop.upload")
public class UploadProperties {

    private Path localDir = Paths.get("uploads");

    private String publicBaseUrl = "http://localhost:8080/uploads";
}
