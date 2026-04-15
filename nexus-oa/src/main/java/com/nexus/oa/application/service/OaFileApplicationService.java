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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OaFileApplicationService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileKey = UUID.randomUUID().toString().replace("-", "") + ext;
        String relativePath = tenantId + "/" + fileKey;
        File dest = new File(uploadDir, relativePath);
        dest.getParentFile().mkdirs();
        file.transferTo(dest.toPath());

        OaFile f = new OaFile();
        f.setTenantId(tenantId);
        f.setFolderId(folderId != null ? folderId : 0L);
        f.setFileName(originalFilename != null ? originalFilename : "未命名文件");
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
            // 文件不存在或无法删除，记录日志但不影响业务
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
        vo.setFileKey(f.getFileKey());
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
        private String fileKey;
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
