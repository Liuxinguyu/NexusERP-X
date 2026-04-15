package com.nexus.common.audit;

/**
 * 由业务模块（如 nexus-system）提供实现，将操作日志写入持久化。
 */
@FunctionalInterface
public interface OperLogRecorder {

    void record(OperLogRecord record);
}
