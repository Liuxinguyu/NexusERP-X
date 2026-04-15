package com.nexus.oa.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.oa.application.service.OaFileApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/oa/files")
@RequiredArgsConstructor
@Validated
public class OaFileController {

    private final OaFileApplicationService service;

    // ===================== 文件夹 =====================

    @GetMapping("/folders")
    public Result<List<OaFileApplicationService.FolderVO>> listFolders(
            @RequestParam(required = false) Long parentId) {
        return Result.ok(service.listFolders(parentId));
    }

    @OpLog(module = "云空间", type = "新建文件夹")
    @PostMapping("/folders")
    public Result<Long> createFolder(@Valid @RequestBody OaFileApplicationService.FolderCreateReq req) {
        return Result.ok(service.createFolder(req));
    }

    @OpLog(module = "云空间", type = "删除文件夹")
    @DeleteMapping("/folders/{id}")
    public Result<Void> deleteFolder(@PathVariable Long id) {
        service.deleteFolder(id);
        return Result.ok();
    }

    // ===================== 文件 =====================

    @GetMapping
    public Result<List<OaFileApplicationService.FileVO>> listFiles(
            @RequestParam(required = false) Long folderId) {
        return Result.ok(service.listFiles(folderId));
    }

    @OpLog(module = "云空间", type = "上传文件")
    @PostMapping("/upload")
    public Result<OaFileApplicationService.FileUploadVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false, defaultValue = "0") Integer visibility) throws IOException {
        return Result.ok(service.upload(file, folderId, visibility));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        String path = service.getFilePath(id);
        service.incrementDownload(id);
        Path filePath = Paths.get(path);
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @OpLog(module = "云空间", type = "删除文件")
    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        service.deleteFile(id);
        return Result.ok();
    }
}
