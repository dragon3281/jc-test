package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.detection.platform.config.GlobalExceptionHandler;
import com.detection.platform.dao.BaseDataMapper;
import com.detection.platform.entity.BaseData;
import com.detection.platform.service.BaseDataService;
import com.detection.platform.vo.BaseDataVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基础数据Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaseDataServiceImpl extends ServiceImpl<BaseDataMapper, BaseData> implements BaseDataService {
    
    @Override
    public Page<BaseDataVO> pageBaseData(Integer current, Integer size, String keyword, Integer dataType) {
        LambdaQueryWrapper<BaseData> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), BaseData::getDataValue, keyword);
        wrapper.eq(dataType != null, BaseData::getDataType, dataType);
        wrapper.orderByDesc(BaseData::getImportTime);
        
        Page<BaseData> page = this.page(new Page<>(current, size), wrapper);
        
        Page<BaseDataVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<BaseDataVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer importTxt(MultipartFile file, String country, String dataType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new GlobalExceptionHandler.BusinessException("文件不能为空");
        }
        
        if (!StringUtils.hasText(country)) {
            throw new GlobalExceptionHandler.BusinessException("国家不能为空");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".txt")) {
            throw new GlobalExceptionHandler.BusinessException("只支持TXT文本文件格式");
        }
        
        List<BaseData> dataList = new ArrayList<>();
        String content = new String(file.getBytes(), "UTF-8");
        String[] lines = content.split("\\r?\\n");
        
        String importBatch = String.valueOf(System.currentTimeMillis());
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }
            
            // 验证是否为纯数字
            if (!line.matches("^\\d+$")) {
                log.warn("第{}行数据不是纯数字，已跳过: {}", i + 1, line);
                continue;
            }
            
            BaseData baseData = new BaseData();
            baseData.setDataValue(line);
            baseData.setAccountIdentifier(line); // 兼容字段
            // 数据类型为可选项，允许为空
            if (StringUtils.hasText(dataType)) {
                baseData.setDataType(dataType);
            }
            baseData.setCountry(country);
            baseData.setDataSource("文本导入");
            baseData.setImportBatch(importBatch);
            baseData.setRemark(filename); // 记录文件名
            
            dataList.add(baseData);
        }
        
        if (dataList.isEmpty()) {
            throw new GlobalExceptionHandler.BusinessException("文本中没有有效的纯数字数据");
        }
        
        // 批量插入
        this.saveBatch(dataList);
        
        log.info("TXT导入成功, 文件名: {}, 国家: {}, 数据类型: {}, 数据量: {}", 
                filename, country, dataType, dataList.size());
        return dataList.size();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchAddBaseData(List<BaseData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        this.saveBatch(dataList);
        
        log.info("批量添加基础数据成功, 数量: {}", dataList.size());
        return dataList.size();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteBaseData(Long id) {
        BaseData data = this.getById(id);
        if (data == null) {
            throw new GlobalExceptionHandler.BusinessException("数据不存在");
        }
        
        boolean success = this.removeById(id);
        
        if (success) {
            log.info("删除基础数据成功, ID: {}", id);
        }
        
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteBaseData(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        
        boolean success = this.removeByIds(ids);
        
        if (success) {
            log.info("批量删除基础数据成功, 数量: {}", ids.size());
        }
        
        return success;
    }
    
    /**
     * 实体转VO
     */
    private BaseDataVO convertToVO(BaseData data) {
        BaseDataVO vo = new BaseDataVO();
        BeanUtils.copyProperties(data, vo);
        return vo;
    }

    @Override
    public Page<com.detection.platform.vo.UploadRecordVO> pageUploadRecords(Integer current, Integer size, String keyword) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<BaseData> qw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.select("import_batch as importBatch",
                "MAX(import_time) as uploadTime",
                "MAX(country) as country",
                "MAX(data_type) as dataType",
                "MAX(remark) as fileName",
                "COUNT(1) as itemCount");
        if (org.springframework.util.StringUtils.hasText(keyword)) {
            qw.like("remark", keyword).or().like("data_type", keyword).or().like("country", keyword);
        }
        qw.groupBy("import_batch").orderByDesc("uploadTime");
        com.baomidou.mybatisplus.core.metadata.IPage<java.util.Map<String,Object>> mapPage = this.baseMapper.selectMapsPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size), qw);
        Page<com.detection.platform.vo.UploadRecordVO> voPage = new Page<>(mapPage.getCurrent(), mapPage.getSize(), mapPage.getTotal());
        java.util.List<com.detection.platform.vo.UploadRecordVO> vos = new java.util.ArrayList<>();
        for (java.util.Map<String,Object> m : mapPage.getRecords()) {
            com.detection.platform.vo.UploadRecordVO vo = new com.detection.platform.vo.UploadRecordVO();
            vo.setImportBatch((String)m.get("importBatch"));
            vo.setCountry((String)m.get("country"));
            vo.setDataType((String)m.get("dataType"));
            vo.setFileName((String)m.get("fileName"));
            Object cnt = m.get("itemCount");
            vo.setItemCount(cnt == null ? 0L : Long.valueOf(cnt.toString()));
            vo.setUploadTime((java.time.LocalDateTime)m.get("uploadTime"));
            vos.add(vo);
        }
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public java.util.List<String> previewUpload(String importBatch, Integer limit) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BaseData> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(BaseData::getImportBatch, importBatch).orderByAsc(BaseData::getId);
        Page<BaseData> page = this.page(new Page<>(1, limit == null ? 10 : limit), wrapper);
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (BaseData bd : page.getRecords()) {
            lines.add(bd.getDataValue());
        }
        return lines;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUploadMeta(String importBatch, String country, String dataType) {
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BaseData> uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        uw.eq(BaseData::getImportBatch, importBatch);
        if (org.springframework.util.StringUtils.hasText(country)) {
            uw.set(BaseData::getCountry, country);
        }
        if (org.springframework.util.StringUtils.hasText(dataType)) {
            uw.set(BaseData::getDataType, dataType);
        }
        return this.update(uw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteUploadBatch(java.util.List<String> batches) {
        if (batches == null || batches.isEmpty()) return false;
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BaseData> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.in(BaseData::getImportBatch, batches);
        return this.remove(wrapper);
    }
}
