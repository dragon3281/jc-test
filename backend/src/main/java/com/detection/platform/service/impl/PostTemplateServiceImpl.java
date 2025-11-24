package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.detection.platform.config.GlobalExceptionHandler;
import com.detection.platform.dao.PostTemplateMapper;
import com.detection.platform.dto.PostTemplateDTO;
import com.detection.platform.entity.PostTemplate;
import com.detection.platform.service.PostTemplateService;
import com.detection.platform.vo.PostTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * POST模板Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostTemplateServiceImpl extends ServiceImpl<PostTemplateMapper, PostTemplate> implements PostTemplateService {
    
    @Override
    public Page<PostTemplateVO> pageTemplates(Integer current, Integer size, String templateName) {
        LambdaQueryWrapper<PostTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(templateName), PostTemplate::getTemplateName, templateName);
        wrapper.orderByDesc(PostTemplate::getCreateTime);
        
        Page<PostTemplate> page = this.page(new Page<>(current, size), wrapper);
        
        Page<PostTemplateVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        
        return voPage;
    }
    
    @Override
    public List<PostTemplateVO> listAllTemplates() {
        return this.list().stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public PostTemplateVO getTemplateById(Long id) {
        PostTemplate template = this.getById(id);
        if (template == null) {
            throw new GlobalExceptionHandler.BusinessException("模板不存在");
        }
        return convertToVO(template);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTemplate(PostTemplateDTO templateDTO) {
        PostTemplate template = new PostTemplate();
        BeanUtils.copyProperties(templateDTO, template);
        template.setVersion("1");
        
        this.save(template);
        log.info("添加POST模板成功, ID: {}, 名称: {}", template.getId(), template.getTemplateName());
        return template.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTemplate(PostTemplateDTO templateDTO) {
        if (templateDTO.getId() == null) {
            throw new GlobalExceptionHandler.BusinessException("模板ID不能为空");
        }
        
        PostTemplate existTemplate = this.getById(templateDTO.getId());
        if (existTemplate == null) {
            throw new GlobalExceptionHandler.BusinessException("模板不存在");
        }
        
        PostTemplate template = new PostTemplate();
        BeanUtils.copyProperties(templateDTO, template);
        Integer version = Integer.parseInt(existTemplate.getVersion());
        template.setVersion(String.valueOf(version + 1));
        
        boolean success = this.updateById(template);
        if (success) {
            log.info("更新POST模板成功, ID: {}", template.getId());
        }
        
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTemplate(Long id) {
        boolean success = this.removeById(id);
        if (success) {
            log.info("删除POST模板成功, ID: {}", id);
        }
        return success;
    }
    
    @Override
    public Boolean testTemplate(Long id, String testData) {
        PostTemplate template = this.getById(id);
        if (template == null) {
            throw new GlobalExceptionHandler.BusinessException("模板不存在");
        }
        
        // TODO: 实现模板测试逻辑
        log.info("测试POST模板, ID: {}, 测试数据: {}", id, testData);
        return true;
    }
    
    private PostTemplateVO convertToVO(PostTemplate template) {
        PostTemplateVO vo = new PostTemplateVO();
        BeanUtils.copyProperties(template, vo);
        return vo;
    }
}
