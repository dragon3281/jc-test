package com.detection.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.entity.RegisterTemplate;

import java.util.List;

public interface RegisterTemplateService {
    Long addFromTask(Long taskId, String templateName, String notes);
    Long addFromDraft(Long draftId, String templateName, String notes);
    Page<RegisterTemplate> pageTemplates(Integer current, Integer size);
    List<RegisterTemplate> listAll();
    RegisterTemplate getById(Long id);
    boolean deleteById(Long id);
}
