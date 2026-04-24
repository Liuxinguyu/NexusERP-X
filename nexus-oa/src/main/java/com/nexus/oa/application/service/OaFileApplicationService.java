package com.nexus.oa.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.SecurityUtils;
import com.nexus.oa.domain.model.OaFile;
import com.nexus.oa.domain.model.OaFileFolder;
import com.nexus.oa.infrastructure.mapper.OaFileFolderMapper;
import com.nexus.oa.infrastructure.mapper.OaFileMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OaFileApplicationService {

    private static final Logger log = LoggerFactory.getLogger(OaFileApplicationService.class);

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".png", ".pdf", ".docx", ".xlsx", ".xls");

    /** 本地文件存储根目录，可在 application.yml 中配置 */
    @Value("${nexus.file.upload-dir:/data/nexus-files}")
    private String uploadDir;

    private final OaFileFolderMapper folderMapper;
    private final OaFileMapper fileMapper;

    // ===================== 文件夹 =====================

    public List<FolderVO> listFolders(Long parentId) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        return folderMapper.selectList(new LambdaQueryWrapper<OaFileFolder>()
                        .eq(OaFileFolder::getTenantId, tenantId)
                        .eq(OaFileFolder::getDelFlag, 0)
                        .eq(parentId != null, OaFileFolder::getParentId, parentId)
                        .and(w -> w
                                .eq(OaFileFolder::getOwnerUserId, userId)
                                .or()
                                .eq(OaFileFolder::getVisibility, 1))
                        .orderByAsc(OaFileFolder::getId))
                .stream().map(this::toFolderVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createFolder(FolderCreateReq req) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        OaFileFolder f = new OaFileFolder();
        f.setTenantId(tenantId);
        f.setParentId(req.getParentId() != null ? req.getParentId() : 0L);
        f.setFolderName(req.getFolderName().trim());
        f.setVisibility(req.getVisibility() != null ? req.getVisibility() : 0);
        f.setOwnerUserId(userId);
        folderMapper.insert(f);
        return f.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long id) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        OaFileFolder f = loadFolder(id, tenantId);
        if (!Objects.equals(f.getOwnerUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有文件夹所有者可以删除");
        }
        folderMapper.deleteById(id);
    }

    // ===================== 文件 =====================

    public List<FileVO> listFiles(Long folderId) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        return fileMapper.selectList(new LambdaQueryWrapper<OaFile>()
                        .eq(OaFile::getTenantId, tenantId)
                        .eq(OaFile::getDelFlag, 0)
                        .eq(folderId != null, OaFile::getFolderId, folderId)
                        .and(w -> w
                                .eq(OaFile::getOwnerUserId, userId)
                                .or()
                                .eq(OaFile::getVisibility, 1))
                        .orderByDesc(OaFile::getId))
                .stream().map(this::toFileVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO upload(MultipartFile file, Long folderId, Integer visibility) throws IOException {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件名不能为空");
        }

        // 仅保留名称部分，剥离客户端可能注入的路径片段
        String safeOriginalFilename = Paths.get(originalFilename).getFileName().toString();
        String ext = "";
        int lastDot = safeOriginalFilename.lastIndexOf(".");
        if (lastDot >= 0) {
            ext = safeOriginalFilename.substring(lastDot).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的文件类型，禁止上传");
        }

        // 强制随机存储名，剥夺前端命名权，防止路径穿越和恶意可执行文件覆盖
        String fileKey = UUID.randomUUID().toString().replace("-", "") + ext;
        Path tenantDir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(tenantId.toString()).normalize();
        Files.createDirectories(tenantDir);
        Path dest = tenantDir.resolve(fileKey).normalize();
        if (!dest.startsWith(tenantDir)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "非法文件路径");
        }
        file.transferTo(dest);

        OaFile f = new OaFile();
        f.setTenantId(tenantId);
        f.setFolderId(folderId != null ? folderId : 0L);
        f.setFileName(safeOriginalFilename);
        f.setFileKey(fileKey);
        f.setFileSize(file.getSize());
        f.setFileType(ext.replace(".", ""));
        f.setVisibility(visibility != null ? visibility : 0);
        f.setOwnerUserId(userId);
        f.setDownloadCount(0);
        fileMapper.insert(f);

        FileUploadVO vo = new FileUploadVO();
        vo.setId(f.getId());
        vo.setFileName(f.getFileName());
        vo.setFileSize(f.getFileSize());
        vo.setFileType(f.getFileType());
        return vo;
    }

    public OaFile getFileInfo(Long id) {
        Long tenantId = requireTenantId();
        return loadFile(id, tenantId);
    }

    public String getFilePath(Long id) {
        Long tenantId = requireTenantId();
        OaFile f = loadFile(id, tenantId);
        Long currentUserId = requireUserId();
        Integer visibility = f.getVisibility();
        if (!Objects.equals(visibility, 1) && !Objects.equals(f.getOwnerUserId(), currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该文件");
        }
        return uploadDir + "/" + tenantId + "/" + f.getFileKey();
    }

    @Transactional(rollbackFor = Exception.class)
    public void incrementDownload(Long id) {
        Long tenantId = requireTenantId();
        OaFile f = loadFile(id, tenantId);
        f.setDownloadCount(f.getDownloadCount() != null ? f.getDownloadCount() + 1 : 1);
        fileMapper.updateById(f);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        OaFile f = loadFile(id, tenantId);
        if (!Objects.equals(f.getOwnerUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有文件所有者可以删除");
        }
        // 删除物理文件
        try {
            Path path = Paths.get(uploadDir, tenantId.toString(), f.getFileKey());
            Files.deleteIfExists(path);
        } catch (java.io.IOException e) {
            log.warn("物理文件删除失败 [tenantId={}, fileKey={}]: {}", tenantId, f.getFileKey(), e.getMessage());
        }
        fileMapper.deleteById(id);
    }

    // ===================== 私有方法 =====================

    private OaFileFolder loadFolder(Long id, Long tenantId) {
        OaFileFolder f = folderMapper.selectById(id);
        if (f == null || !Objects.equals(f.getTenantId(), tenantId)
                || (f.getDelFlag() != null && f.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件夹不存在");
        }
        return f;
    }

    private OaFile loadFile(Long id, Long tenantId) {
        OaFile f = fileMapper.selectById(id);
        if (f == null || !Objects.equals(f.getTenantId(), tenantId)
                || (f.getDelFlag() != null && f.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        return f;
    }

    private FolderVO toFolderVO(OaFileFolder f) {
        FolderVO vo = new FolderVO();
        vo.setId(f.getId());
        vo.setParentId(f.getParentId());
        vo.setFolderName(f.getFolderName());
        vo.setVisibility(f.getVisibility());
        vo.setOwnerUserId(f.getOwnerUserId());
        return vo;
    }

    private FileVO toFileVO(OaFile f) {
        FileVO vo = new FileVO();
        vo.setId(f.getId());
        vo.setFolderId(f.getFolderId());
        vo.setFileName(f.getFileName());
        vo.setFileSize(f.getFileSize());
        vo.setFileType(f.getFileType());
        vo.setDownloadCount(f.getDownloadCount());
        vo.setVisibility(f.getVisibility());
        vo.setOwnerUserId(f.getOwnerUserId());
        return vo;
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        return tid;
    }

    private static Long requireUserId() {
        Long uid = SecurityUtils.currentUserId();
        if (uid == null) throw new BusinessException(ResultCode.BAD_REQUEST, "缺少用户上下文");
        return uid;
    }

    // ===================== DTO =====================

    @lombok.Data
    public static class FolderCreateReq implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long parentId;
        @jakarta.validation.constraints.NotBlank
        private String folderName;
        private Integer visibility;
    }

    @lombok.Data
    public static class FolderVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long parentId;
        private String folderName;
        private Integer visibility;
        private Long ownerUserId;
    }

    @lombok.Data
    public static class FileVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long folderId;
        private String fileName;
        private Long fileSize;
        private String fileType;
        private Integer downloadCount;
        private Integer visibility;
        private Long ownerUserId;
    }

    @lombok.Data
    public static class FileUploadVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String fileName;
        private Long fileSize;
        private String fileType;
    }
}
