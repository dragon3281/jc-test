package com.detection.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.common.utils.Result;
import com.detection.platform.dto.PostTemplateDTO;
import com.detection.platform.service.PostTemplateService;
import com.detection.platform.vo.PostTemplateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * POST模板Controller
 */
@RestController
@RequestMapping("/template")
@RequiredArgsConstructor
public class PostTemplateController {
    
    private final PostTemplateService postTemplateService;
    
    @GetMapping("/page")
    public Result<Page<PostTemplateVO>> pageTemplates(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String templateName) {
        Page<PostTemplateVO> page = postTemplateService.pageTemplates(current, size, templateName);
        return Result.success(page);
    }
    
    @GetMapping("/list")
    public Result<List<PostTemplateVO>> listAllTemplates() {
        List<PostTemplateVO> list = postTemplateService.listAllTemplates();
        return Result.success(list);
    }
    
    @GetMapping("/{id}")
    public Result<PostTemplateVO> getTemplateById(@PathVariable Long id) {
        PostTemplateVO template = postTemplateService.getTemplateById(id);
        return Result.success(template);
    }
    
    @PostMapping
    public Result<Long> addTemplate(@Valid @RequestBody PostTemplateDTO templateDTO) {
        Long id = postTemplateService.addTemplate(templateDTO);
        return Result.success("添加模板成功", id);
    }
    
    @PutMapping
    public Result<Void> updateTemplate(@Valid @RequestBody PostTemplateDTO templateDTO) {
        postTemplateService.updateTemplate(templateDTO);
        return Result.successMsg("更新模板成功");
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        postTemplateService.deleteTemplate(id);
        return Result.successMsg("删除模板成功");
    }
    
    @PostMapping("/{id}/test")
    public Result<Boolean> testTemplate(@PathVariable Long id, @RequestBody String testData) {
        Boolean result = postTemplateService.testTemplate(id, testData);
        return Result.success("测试完成", result);
    }
}
