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
    private static final List<String> ALLOWED = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".mp4", ".mov", ".webm");

    private final Path baseDir;

    public MediaController(@Value("${media.base-path:media}") String basePath) throws IOException {
        this.baseDir = Path.of(basePath).toAbsolutePath().normalize();
        Files.createDirectories(baseDir); // task 4: creeaza folderul la pornire daca lipseste
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        // 1. fisierul nu are voie sa fie gol
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        // 2. extensia din numele original, cu litere mici (ex: "Video.MP4" -> ".mp4")
        String original = file.getOriginalFilename();
        String extension = "";
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        if (!ALLOWED.contains(extension)) {
            throw new BadRequestException("File type not allowed");
        }

        // 3. nume nou: UUID + extensie (numele trimis de utilizator nu ajunge niciodata pe disc)
        String filename = UUID.randomUUID() + extension;

        // 4. salvam fisierul in folder
        Files.copy(file.getInputStream(), baseDir.resolve(filename));

        // 5. raspuns 201 cu { "mediaUrl": "/media/<filename>" }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("mediaUrl", "/media/" + filename));
    }
}