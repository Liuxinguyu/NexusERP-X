package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.SecurityUtils;
import com.nexus.system.domain.model.SysMessage;
import com.nexus.system.infrastructure.mapper.SysMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class SysMessageApplicationService {

    public static final String TYPE_APPROVAL = "APPROVAL";
    public static final String TYPE_SYSTEM = "SYSTEM";
    public static final String TYPE_NOTICE = "NOTICE";

    private final SysMessageMapper messageMapper;

    public SysMessageApplicationService(SysMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void sendToUser(Long tenantId, Long userId, String title, String content, String messageType) {
        SysMessage m = new SysMessage();
        m.setTenantId(tenantId);
        m.setUserId(userId);
        m.setTitle(title);
        m.setContent(content);
        m.setMessageType(messageType);
        m.setIsRead(0);
        messageMapper.insert(m);
    }

    public long unreadCount() {
        Long uid = requireUserId();
        Long tid = requireTenantId();
        return messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getTenantId, tid)
                .eq(SysMessage::getUserId, uid)
                .eq(SysMessage::getIsRead, 0)
                .eq(SysMessage::getDelFlag, 0));
    }

    public IPage<SysMessage> pageUnread(long pageNum, long pageSize) {
        Long uid = requireUserId();
        Long tid = requireTenantId();
        return messageMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getTenantId, tid)
                        .eq(SysMessage::getUserId, uid)
                        .eq(SysMessage::getIsRead, 0)
                        .eq(SysMessage::getDelFlag, 0)
                        .orderByDesc(SysMessage::getCreateTime));
    }

    /**
     * 查询全部消息（分页）。
     */
    public IPage<SysMessage> pageAll(long pageNum, long pageSize) {
        Long uid = requireUserId();
        Long tid = requireTenantId();
        return messageMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getTenantId, tid)
                        .eq(SysMessage::getUserId, uid)
                        .eq(SysMessage::getDelFlag, 0)
                        .orderByDesc(SysMessage::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        Long uid = requireUserId();
        Long tid = requireTenantId();
        messageMapper.update(null, new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getTenantId, tid)
                .eq(SysMessage::getUserId, uid)
                .eq(SysMessage::getDelFlag, 0)
                .set(SysMessage::getIsRead, 1));
    }

    private static Long requireUserId() {
        Long uid = SecurityUtils.currentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        return uid;
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
