package com.detection.platform.service;

import com.detection.platform.entity.EncryptionExecutor;

import java.util.List;

public interface EncryptionExecutorService {
    List<EncryptionExecutor> listAll();
    List<EncryptionExecutor> listBuiltin();
    EncryptionExecutor getById(Long id);
    Long createCustomExecutor(EncryptionExecutor executor);
}
