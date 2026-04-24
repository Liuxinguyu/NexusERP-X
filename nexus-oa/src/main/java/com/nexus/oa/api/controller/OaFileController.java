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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    @PreAuthorize("@ss.hasPermi('oa:cloud-disk:list')")
    public Result<List<OaFileApplicationService.FolderVO>> listFolders(
            @RequestParam(required = false) Long parentId) {
        return Result.ok(service.listFolders(parentId));
    }

    @OpLog(module = "云空间", type = "新建文件夹")
    @PreAuthorize("@ss.hasPermi('oa:cloud-disk:list')")
    @PostMapping("/folders")
    public Result<Long> createFolder(@Valid @RequestBody OaFileApplicationService.FolderCreateReq req) {
        return Result.ok(service.createFolder(req));
    }

    @OpLog(module = "云空间", type = "删除文件夹")
    @PreAuthorize("@ss.hasPermi('oa:file:delete')")
    @DeleteMapping("/folders/{id}")
    public Result<Void> deleteFolder(@PathVariable Long id) {
        service.deleteFolder(id);
        return Result.ok();
    }

    // ===================== 文件 =====================

    @GetMapping
    @PreAuthorize("@ss.hasPermi('oa:cloud-disk:list')")
    public Result<List<OaFileApplicationService.FileVO>> listFiles(
            @RequestParam(required = false) Long folderId) {
        return Result.ok(service.listFiles(folderId));
    }

    @OpLog(module = "云空间", type = "上传文件")
    @PreAuthorize("@ss.hasPermi('oa:cloud-disk:list')")
    @PostMapping("/upload")
    public Result<OaFileApplicationService.FileUploadVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false, defaultValue = "0") Integer visibility) throws IOException {
        return Result.ok(service.upload(file, folderId, visibility));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("@ss.hasPermi('oa:cloud-disk:list')")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        String path = service.getFilePath(id);
        service.incrementDownload(id);
        Path filePath = Paths.get(path);
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String encodedName = URLEncoder.encode(filePath.getFileName().toString(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @OpLog(module = "云空间", type = "删除文件")
    @PreAuthorize("@ss.hasPermi('oa:file:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        service.deleteFile(id);
        return Result.ok();
    }
}
