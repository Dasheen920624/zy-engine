package com.medkernel.datagovernance.service;

import com.medkernel.datagovernance.entity.DoctorEntity;
import com.medkernel.datagovernance.repository.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 医生主数据服务
 */
@Service
public class DoctorService {
    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    /**
     * 保存医生主数据
     */
    public DoctorEntity save(DoctorEntity entity) {
        if (entity.getCreatedTime() == null) {
            entity.setCreatedTime(LocalDateTime.now());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        doctorRepository.save(entity);
        return entity;
    }

    /**
     * 根据医生ID查找医生
     */
    public DoctorEntity findByDoctorId(String tenantId, String doctorId) {
        return doctorRepository.findByDoctorId(tenantId, doctorId);
    }

    /**
     * 根据租户ID查找所有医生
     */
    public List<DoctorEntity> findAllByTenantId(String tenantId) {
        return doctorRepository.findAllByTenantId(tenantId);
    }
}