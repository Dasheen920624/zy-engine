package com.medkernel.datagovernance.service;

import com.medkernel.datagovernance.entity.DepartmentEntity;
import com.medkernel.datagovernance.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 科室主数据服务
 */
@Service
public class DepartmentService {
    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    /**
     * 保存科室主数据
     */
    public DepartmentEntity save(DepartmentEntity entity) {
        if (entity.getCreatedTime() == null) {
            entity.setCreatedTime(LocalDateTime.now());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        departmentRepository.save(entity);
        return entity;
    }

    /**
     * 根据科室编码查找科室
     */
    public DepartmentEntity findByDeptCode(String tenantId, String deptCode) {
        return departmentRepository.findByDeptCode(tenantId, deptCode);
    }

    /**
     * 根据租户ID查找所有科室
     */
    public List<DepartmentEntity> findAllByTenantId(String tenantId) {
        return departmentRepository.findAllByTenantId(tenantId);
    }
}