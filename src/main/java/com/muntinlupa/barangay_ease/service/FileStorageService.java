package com.muntinlupa.barangay_ease.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_FOLDERS = Set.of("complaints", "service-requests");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.root = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    public String save(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        if (!ALLOWED_FOLDERS.contains(folder)) {
            throw new IllegalArgumentException("Invalid upload destination.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Each uploaded file must be 10 MB or smaller.");
        }

        String originalName = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only PDF, JPG, JPEG and PNG files are allowed.");
        }
        validateContentSignature(file, extension);

        Path directory = root.resolve(folder).normalize();
        if (!directory.startsWith(root)) {
            throw new IOException("Invalid upload destination.");
        }
        Files.createDirectories(directory);

        String safeName = UUID.randomUUID() + "_" + Paths.get(originalName).getFileName()
                .toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = directory.resolve(safeName).normalize();
        if (!target.startsWith(directory)) {
            throw new IOException("Invalid file path.");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return root.relativize(target).toString().replace('\\', '/');
    }

    public Path resolve(String relative) throws IOException {
        if (relative == null || relative.isBlank()) {
            throw new IOException("File path is missing.");
        }
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new IOException("File does not exist.");
        }
        return path;
    }

    public void delete(String relative) {
        if (relative == null || relative.isBlank()) return;
        try {
            Path path = root.resolve(relative).normalize();
            if (path.startsWith(root)) Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Cleanup must not hide the original database or upload error.
        }
    }

    private String extensionOf(String filename) {
        String clean = Paths.get(filename).getFileName().toString();
        int dot = clean.lastIndexOf('.');
        return dot < 0 ? "" : clean.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void validateContentSignature(MultipartFile file, String extension) throws IOException {
        byte[] header = file.getBytes();
        boolean valid = switch (extension) {
            case "pdf" -> header.length >= 5 && header[0] == '%' && header[1] == 'P'
                    && header[2] == 'D' && header[3] == 'F' && header[4] == '-';
            case "png" -> header.length >= 8 && (header[0] & 0xff) == 0x89
                    && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                    && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a;
            case "jpg", "jpeg" -> header.length >= 3 && (header[0] & 0xff) == 0xff
                    && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException("The uploaded file content does not match its file type.");
    }
}
