package com.instakill.media.api.rest;

import com.instakill.common.error.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.instakill.media.processing.ImageProcessingService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    // extensiile acceptate
    private static final List<String> ALLOWED = List.of(".jpg", ".jpeg", ".png", ".gif", ".mp4", ".mov", ".webm");
    private static final List<String> IMAGE_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".gif");

    private final ImageProcessingService imageProcessingService;
    private final Path baseDir;

    public MediaController(@Value("${media.base-path:media}") String basePath,
                       ImageProcessingService imageProcessingService) throws IOException {
    this.baseDir = Path.of(basePath).toAbsolutePath().normalize();
    this.imageProcessingService = imageProcessingService;
    Files.createDirectories(baseDir);
}

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String original = file.getOriginalFilename();
        String extension = "";
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        if (!ALLOWED.contains(extension)) {
            throw new BadRequestException("File type not allowed");
        }

        String filename = UUID.randomUUID() + extension;

        Path target = baseDir.resolve(filename);
        if (IMAGE_EXTENSIONS.contains(extension)) {
            Files.write(target, imageProcessingService.process(file.getInputStream(), extension.substring(1)));
        } else {
            Files.copy(file.getInputStream(), target); // video: se salveaza ca atare
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("mediaUrl", "/media/" + filename));
    }
}