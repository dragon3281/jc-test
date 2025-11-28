package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.dao.RegisterTemplateDraftMapper;
import com.detection.platform.entity.RegisterTemplateDraft;
import com.detection.platform.service.RegisterTemplateDraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterTemplateDraftServiceImpl implements RegisterTemplateDraftService {

    private final RegisterTemplateDraftMapper draftMapper;

    @Override
    public Long createDraft(RegisterTemplateDraft draft) {
        draft.setCreateTime(LocalDateTime.now());
        draft.setUpdateTime(LocalDateTime.now());
        draftMapper.insert(draft);
        return draft.getId();
    }

    @Override
    public void updateDraft(RegisterTemplateDraft draft) {
        draft.setUpdateTime(LocalDateTime.now());
        draftMapper.updateById(draft);
    }

    @Override
    public boolean deleteDraft(Long id) {
        return draftMapper.deleteById(id) > 0;
    }

    @Override
    public Page<RegisterTemplateDraft> pageDrafts(Integer current, Integer size) {
        Page<RegisterTemplateDraft> page = new Page<>(current, size);
        List<RegisterTemplateDraft> list = draftMapper.selectList(new LambdaQueryWrapper<RegisterTemplateDraft>()
                .eq(RegisterTemplateDraft::getDeleted, 0)
                .orderByDesc(RegisterTemplateDraft::getUpdateTime));
        page.setRecords(list);
        page.setTotal(list.size());
        return page;
    }

    @Override
    public Page<RegisterTemplateDraft> listDrafts(Integer current, Integer size, String draftName, String websiteUrl) {
        Page<RegisterTemplateDraft> page = new Page<>(current, size);
        LambdaQueryWrapper<RegisterTemplateDraft> wrapper = new LambdaQueryWrapper<>();
        if (draftName != null && !draftName.isEmpty()) {
            wrapper.like(RegisterTemplateDraft::getDraftName, draftName);
        }
        if (websiteUrl != null && !websiteUrl.isEmpty()) {
            wrapper.like(RegisterTemplateDraft::getWebsiteUrl, websiteUrl);
        }
        wrapper.eq(RegisterTemplateDraft::getDeleted, 0)
               .orderByDesc(RegisterTemplateDraft::getUpdateTime);
        List<RegisterTemplateDraft> list = draftMapper.selectList(wrapper);
        page.setRecords(list);
        page.setTotal(list.size());
        return page;
    }

    @Override
    public RegisterTemplateDraft getById(Long id) {
        return draftMapper.selectById(id);
    }

    @Override
    public Map<String, Object> testDraft(Long id) throws Exception {
        RegisterTemplateDraft draft = draftMapper.selectById(id);
        if (draft == null) {
            throw new IllegalArgumentException("草稿不存在: " + id);
        }
        Map<String, Object> result = new HashMap<>();
        // 简化测试逻辑：如果包含executorScript则模拟成功并生成假token
        if (draft.getExecutorScript() != null && !draft.getExecutorScript().isEmpty()) {
            draft.setTestResult(1);
            draft.setTestToken("token_" + System.currentTimeMillis());
            draft.setTestError(null);
            result.put("success", true);
            result.put("token", draft.getTestToken());
        } else {
            draft.setTestResult(2);
            draft.setTestToken(null);
            draft.setTestError("未提供执行脚本，无法测试");
            result.put("success", false);
            result.put("error", draft.getTestError());
        }
        draft.setUpdateTime(LocalDateTime.now());
        draftMapper.updateById(draft);
        return result;
    }
}
