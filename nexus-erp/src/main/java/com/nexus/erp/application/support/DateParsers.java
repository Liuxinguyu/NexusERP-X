package com.nexus.erp.application.support;

import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateParsers {

    private DateParsers() {
    }

    public static LocalDate parseIsoDate(String value, String fieldLabel) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldLabel + "格式非法，必须为yyyy-MM-dd");
        }
    }
}
