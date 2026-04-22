package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.SecurityUtils;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysNotice;
import com.nexus.system.infrastructure.mapper.SysNoticeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class SysNoticeApplicationService {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;

    private final SysNoticeMapper noticeMapper;
    private final AdminAuthorizationService adminAuthorizationService;

    public SysNoticeApplicationService(SysNoticeMapper noticeMapper,
                                       AdminAuthorizationService adminAuthorizationService) {
        this.noticeMapper = noticeMapper;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    public IPage<SysNotice> page(long pageNum, long pageSize) {
        Long tid = requireTenantId();
        return noticeMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysNotice>()
                        .eq(SysNotice::getTenantId, tid)
                        .eq(SysNotice::getDelFlag, 0)
                        .orderByDesc(SysNotice::getCreateTime));
    }

    /**
     * 首页：最新一条已发布且未过期公告标题。
     */
    public String latestPublishedTitle() {
        Long tid = requireTenantId();
        LocalDateTime now = LocalDateTime.now();
        SysNotice one = noticeMapper.selectOne(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getTenantId, tid)
                .eq(SysNotice::getDelFlag, 0)
                .eq(SysNotice::getStatus, STATUS_PUBLISHED)
                .and(w -> w.isNull(SysNotice::getExpireTime).or().gt(SysNotice::getExpireTime, now))
                .orderByDesc(SysNotice::getCreateTime)
                .last("LIMIT 1"));
        return one == null ? null : one.getTitle();
    }

    /**
     * 保存或更新公告。使用 DTO 接收参数，防止恶意注入其他租户的数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdate(SystemAdminDtos.NoticeSaveRequest req) {
        requireAdmin();
        Long tid = requireTenantId();
        Long uid = SecurityUtils.currentUserId();

        if (req.getId() == null) {
            SysNotice n = new SysNotice();
            n.setTenantId(tid);
            n.setTitle(req.getTitle());
            n.setContent(req.getContent());
            n.setNoticeType(req.getNoticeType());
            n.setExpireTime(req.getExpireTime());
            n.setStatus(STATUS_DRAFT);
            n.setCreateBy(uid);
            noticeMapper.insert(n);
            return n.getId();
        }

        SysNotice db = noticeMapper.selectById(req.getId());
        if (db == null || !Objects.equals(db.getTenantId(), tid) || (db.getDelFlag() != null && db.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        db.setTitle(req.getTitle());
        db.setContent(req.getContent());
        db.setNoticeType(req.getNoticeType());
        db.setExpireTime(req.getExpireTime());
        db.setUpdateBy(uid);
        noticeMapper.updateById(db);
        return db.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        requireAdmin();
        Long tid = requireTenantId();
        SysNotice db = noticeMapper.selectById(id);
        if (db == null || !Objects.equals(db.getTenantId(), tid) || (db.getDelFlag() != null && db.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        db.setStatus(STATUS_PUBLISHED);
        db.setUpdateBy(SecurityUtils.currentUserId());
        noticeMapper.updateById(db);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireAdmin();
        Long tid = requireTenantId();
        SysNotice db = noticeMapper.selectById(id);
        if (db == null || !Objects.equals(db.getTenantId(), tid) || (db.getDelFlag() != null && db.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        db.setUpdateBy(SecurityUtils.currentUserId());
        noticeMapper.updateById(db);
        noticeMapper.deleteById(id);
    }

    private void requireAdmin() {
        Long uid = SecurityUtils.currentUserId();
        if (uid == null || !adminAuthorizationService.hasAdminRole(uid)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可操作");
        }
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
