package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.dao.RegisterTemplateMapper;
import com.detection.platform.dao.RegisterTemplateDraftMapper;
import com.detection.platform.entity.RegisterTask;
import com.detection.platform.entity.RegisterTemplate;
import com.detection.platform.entity.RegisterTemplateDraft;
import com.detection.platform.service.RegisterTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterTemplateServiceImpl implements RegisterTemplateService {

    private final RegisterTemplateMapper registerTemplateMapper;
    private final RegisterTaskServiceImpl registerTaskServiceImpl;
    private final RegisterTemplateDraftMapper draftMapper;

    @Override
    public Long addFromTask(Long taskId, String templateName, String notes) {
        RegisterTask task = registerTaskServiceImpl.getTaskById(taskId);
        RegisterTemplate t = new RegisterTemplate();
        t.setTemplateName(templateName != null ? templateName : task.getTaskName());
        t.setWebsiteUrl(task.getWebsiteUrl());
        t.setRegisterApi(task.getRegisterApi());
        t.setMethod(task.getMethod());
        t.setUsernameField(task.getUsernameField());
        t.setPasswordField(task.getPasswordField());
        t.setDefaultPassword(task.getDefaultPassword());
        
        // 只保存关键的额外参数，避免字段过大
        String extraParams = task.getExtraParams();
        if (extraParams != null && extraParams.length() > 1000) {
            // 截取前1000字符或只保存关键字段
            extraParams = extraParams.substring(0, Math.min(1000, extraParams.length()));
        }
        t.setExtraParams(extraParams);
        
        t.setEncryptionType(task.getEncryptionType());
        t.setRsaKeyApi(task.getRsaKeyApi());
        t.setRsaTsParam(task.getRsaTsParam());
        t.setEncryptionHeader(task.getEncryptionHeader());
        t.setValueFieldName(task.getValueFieldName());
        
        // 智能生成关键逻辑备注
        String autoNotes = generateAutoNotes(task);
        t.setNotes(notes != null && !notes.isEmpty() ? notes : autoNotes);
        
        t.setCreateTime(LocalDateTime.now());
        registerTemplateMapper.insert(t);
        return t.getId();
    }
    
    @Override
    public Long addFromDraft(Long draftId, String templateName, String notes) {
        RegisterTemplateDraft draft = draftMapper.selectById(draftId);
        if (draft == null) {
            throw new IllegalArgumentException("草稿不存在: " + draftId);
        }
        
        RegisterTemplate t = new RegisterTemplate();
        t.setTemplateName(templateName != null ? templateName : draft.getDraftName());
        t.setWebsiteUrl(draft.getWebsiteUrl());
        t.setRegisterApi(draft.getRegisterApi());
        t.setMethod(draft.getMethod());
        t.setUsernameField(draft.getUsernameField());
        t.setPasswordField(draft.getPasswordField());
        t.setDefaultPassword(draft.getDefaultPassword());
        t.setExtraParams(draft.getExtraParams());
        t.setEncryptionType(draft.getEncryptionType());
        t.setRsaKeyApi(draft.getRsaKeyApi());
        t.setRsaTsParam(draft.getRsaTsParam());
        t.setEncryptionHeader(draft.getEncryptionHeader());
        t.setValueFieldName(draft.getValueFieldName());
        
        // 使用草稿的自动备注或用户输入的备注
        String finalNotes = notes != null && !notes.isEmpty() ? notes : draft.getAutoNotes();
        t.setNotes(finalNotes);
        
        t.setCreateTime(LocalDateTime.now());
        registerTemplateMapper.insert(t);
        return t.getId();
    }

    /**
     * 智能生成关键逻辑备注（记录该网站使用的方式与注意点）
     */
    private String generateAutoNotes(RegisterTask task) {
        StringBuilder notes = new StringBuilder();
        
        // 1. 加密方式（记录实际使用的方式）
        String encType = task.getEncryptionType();
        if ("DES_RSA".equalsIgnoreCase(encType)) {
            notes.append("【加密方式】检测到使用 DES+RSA\n");
            notes.append("- RSA：使用老式JS-RSA库（encryptedString + RSAKeyPair），加密原始rnd，输出十六进制\n");
            notes.append("- DES：ECB/PKCS5，密钥=反转rnd后前8字节，输出Base64\n");
            notes.append("- 请求头：" + task.getEncryptionHeader() + "=RSA密文\n");
            notes.append("- 请求体：{\"" + task.getValueFieldName() + "\":\"DES密文\"}\n\n");
        } else {
            notes.append("【加密方式】检测到不使用加密\n\n");
        }
        
        // 2. 请求信息
        notes.append("【请求信息】\n");
        notes.append("- 方法：" + task.getMethod() + "\n");
        notes.append("- 接口：" + task.getRegisterApi() + "\n");
        if ("DES_RSA".equalsIgnoreCase(encType)) {
            notes.append("- RSA密钥接口：" + task.getRsaKeyApi() + "?" + task.getRsaTsParam() + "={timestamp}\n");
        }
        notes.append("\n");
        
        // 3. 必填字段
        notes.append("【必填字段】\n");
        notes.append("- " + task.getUsernameField() + "：自动生成\n");
        notes.append("- " + task.getPasswordField() + "：" + task.getDefaultPassword() + "\n");
        notes.append("- confirmPassword：" + task.getDefaultPassword() + "（需与password一致）\n");
        notes.append("\n");
        
        // 4. 注意事项（记录服务器行为偏好）
        notes.append("【注意事项】\n");
        notes.append("- Content-Type 建议携带 charset=utf-8\n");
        notes.append("- 请求体建议使用紧凑JSON（无空格）\n");
        if ("DES_RSA".equalsIgnoreCase(encType)) {
            notes.append("- 为与前端一致，RSA推荐使用Python脚本生成密文\n");
        }
        
        return notes.toString();
    }

    @Override
    public Page<RegisterTemplate> pageTemplates(Integer current, Integer size) {
        Page<RegisterTemplate> page = new Page<>(current, size);
        List<RegisterTemplate> list = registerTemplateMapper.selectList(null);
        page.setRecords(list);
        page.setTotal(list.size());
        return page;
    }

    @Override
    public RegisterTemplate getById(Long id) {
        return registerTemplateMapper.selectById(id);
    }

    @Override
    public List<RegisterTemplate> listAll() {
        return registerTemplateMapper.selectList(null);
    }

    @Override
    public boolean deleteById(Long id) {
        return registerTemplateMapper.deleteById(id) > 0;
    }
}