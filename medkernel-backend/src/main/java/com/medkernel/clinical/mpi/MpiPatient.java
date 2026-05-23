package com.medkernel.clinical.mpi;

/**
 * MPI 患者主索引 DTO（Record 不可变）。
 */
public record MpiPatient(
    String mpiId,
    String maskedName,
    String gender,
    Integer age,
    String idLast4,
    Integer mergedCount,
    String status
) {}
