package com.detection.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.detection.platform.dto.PostTemplateDTO;
import com.detection.platform.entity.PostTemplate;
import com.detection.platform.vo.PostTemplateVO;

import java.util.List;

/**
 * POST模板Service接口
 */
public interface PostTemplateService extends IService<PostTemplate> {
    
    Page<PostTemplateVO> pageTemplates(Integer current, Integer size, String templateName);
    
    List<PostTemplateVO> listAllTemplates();
    
    PostTemplateVO getTemplateById(Long id);
    
    Long addTemplate(PostTemplateDTO templateDTO);
    
    Boolean updateTemplate(PostTemplateDTO templateDTO);
    
    Boolean deleteTemplate(Long id);
    
    Boolean testTemplate(Long id, String testData);
}
