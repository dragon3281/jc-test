package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.detection.platform.dao.EncryptionExecutorMapper;
import com.detection.platform.entity.EncryptionExecutor;
import com.detection.platform.service.EncryptionExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EncryptionExecutorServiceImpl implements EncryptionExecutorService {

    private final EncryptionExecutorMapper mapper;

    @Override
    public List<EncryptionExecutor> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<EncryptionExecutor>()
                .eq(EncryptionExecutor::getDeleted, 0));
    }

    @Override
    public List<EncryptionExecutor> listBuiltin() {
        return mapper.selectList(new LambdaQueryWrapper<EncryptionExecutor>()
                .eq(EncryptionExecutor::getIsBuiltin, 1)
                .eq(EncryptionExecutor::getDeleted, 0));
    }

    @Override
    public EncryptionExecutor getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public Long createCustomExecutor(EncryptionExecutor executor) {
        executor.setIsBuiltin(0);
        executor.setCreateTime(LocalDateTime.now());
        executor.setUpdateTime(LocalDateTime.now());
        mapper.insert(executor);
        return executor.getId();
    }
}
