package com.detection.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.entity.RegisterTemplateDraft;

import java.util.Map;

public interface RegisterTemplateDraftService {
    Long createDraft(RegisterTemplateDraft draft);
    void updateDraft(RegisterTemplateDraft draft);
    boolean deleteDraft(Long id);
    Page<RegisterTemplateDraft> pageDrafts(Integer current, Integer size);
    Page<RegisterTemplateDraft> listDrafts(Integer current, Integer size, String draftName, String websiteUrl);
    RegisterTemplateDraft getById(Long id);
    Map<String, Object> testDraft(Long id) throws Exception;
}
