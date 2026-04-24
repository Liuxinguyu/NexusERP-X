package com.nexus.erp.application.service;

import com.nexus.common.context.TenantContext;
import com.nexus.erp.domain.model.FinPayable;
import com.nexus.erp.domain.model.FinReceivable;
import com.nexus.erp.infrastructure.mapper.ErpCustomerMapper;
import com.nexus.erp.infrastructure.mapper.ErpPurchaseOrderMapper;
import com.nexus.erp.infrastructure.mapper.ErpSaleOrderMapper;
import com.nexus.erp.infrastructure.mapper.ErpSupplierMapper;
import com.nexus.erp.infrastructure.mapper.FinPayableMapper;
import com.nexus.erp.infrastructure.mapper.FinPayableRecordMapper;
import com.nexus.erp.infrastructure.mapper.FinReceivableMapper;
import com.nexus.erp.infrastructure.mapper.FinReceivableRecordMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class FinanceSourceUniquenessTest {

    @Autowired
    private FinReceivableApplicationService receivableService;

    @Autowired
    private FinPayableApplicationService payableService;

    @MockBean
    private FinReceivableMapper receivableMapper;

    @MockBean
    private FinPayableMapper payableMapper;

    @MockBean
    private FinReceivableRecordMapper finReceivableRecordMapper;

    @MockBean
    private FinPayableRecordMapper finPayableRecordMapper;

    @MockBean
    private ErpSaleOrderMapper erpSaleOrderMapper;

    @MockBean
    private ErpPurchaseOrderMapper erpPurchaseOrderMapper;

    @MockBean
    private ErpCustomerMapper erpCustomerMapper;

    @MockBean
    private ErpSupplierMapper erpSupplierMapper;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void createFromSaleOrderReturnsExistingRowWhenDuplicateKeyOccurs() {
        TenantContext.setTenantId(1L);

        var saleOrder = new com.nexus.erp.domain.model.ErpSaleOrder();
        saleOrder.setId(100L);
        saleOrder.setTenantId(1L);
        saleOrder.setCustomerId(8L);
        saleOrder.setTotalAmount(new BigDecimal("88.00"));

        var customer = new com.nexus.erp.domain.model.ErpCustomer();
        customer.setId(8L);
        customer.setTenantId(1L);
        customer.setName("客户A");

        var existing = new FinReceivable();
        existing.setId(999L);
        existing.setTenantId(1L);
        existing.setSourceType("sale_order");
        existing.setSourceId(100L);

        when(receivableMapper.selectOne(any())).thenReturn(null, existing);
        when(erpSaleOrderMapper.selectById(100L)).thenReturn(saleOrder);
        when(erpCustomerMapper.selectById(8L)).thenReturn(customer);
        doThrow(new DuplicateKeyException("duplicate")).when(receivableMapper).insert(any(FinReceivable.class));

        Long result = receivableService.createFromSaleOrder(100L);

        assertThat(result).isEqualTo(999L);
        Mockito.verify(receivableMapper).insert(any(FinReceivable.class));
    }

    @Test
    void createFromPurchaseOrderReturnsExistingRowWhenDuplicateKeyOccurs() {
        TenantContext.setTenantId(1L);

        var order = new com.nexus.erp.domain.model.ErpPurchaseOrder();
        order.setId(200L);
        order.setTenantId(1L);
        order.setSupplierId(6L);
        order.setTotalAmount(new BigDecimal("66.00"));

        var supplier = new com.nexus.erp.domain.model.ErpSupplier();
        supplier.setId(6L);
        supplier.setTenantId(1L);
        supplier.setSupplierName("供应商A");

        var existing = new FinPayable();
        existing.setId(888L);
        existing.setTenantId(1L);
        existing.setSourceType("purchase_order");
        existing.setSourceId(200L);

        when(payableMapper.selectOne(any())).thenReturn(null, existing);
        when(erpPurchaseOrderMapper.selectById(200L)).thenReturn(order);
        when(erpSupplierMapper.selectById(6L)).thenReturn(supplier);
        doThrow(new DuplicateKeyException("duplicate")).when(payableMapper).insert(any(FinPayable.class));

        Long result = payableService.createFromPurchaseOrder(200L);

        assertThat(result).isEqualTo(888L);
        Mockito.verify(payableMapper).insert(any(FinPayable.class));
    }

    @TestConfiguration
    static class NoOpTransactionManagerConfig {

        @Bean
        @Primary
        PlatformTransactionManager platformTransactionManager() {
            return new PlatformTransactionManager() {
                @Override
                public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                    return new SimpleTransactionStatus();
                }

                @Override
                public void commit(TransactionStatus status) throws TransactionException {
                }

                @Override
                public void rollback(TransactionStatus status) throws TransactionException {
                }
            };
        }
    }
}
