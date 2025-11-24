package com.detection.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.detection.platform.entity.BaseData;
import com.detection.platform.vo.BaseDataVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 基础数据Service接口
 */
public interface BaseDataService extends IService<BaseData> {
    
    /**
     * 分页查询基础数据
     */
    Page<BaseDataVO> pageBaseData(Integer current, Integer size, String keyword, Integer dataType);
    
    /**
     * 导入TXT文本数据
     * @param file TXT文件，每行一个纯数字号码
     * @param country 国家
     * @param dataType 数据类型
     */
    Integer importTxt(MultipartFile file, String country, String dataType) throws IOException;
    
    /**
     * 上传记录分页（VO）
     */
    Page<com.detection.platform.vo.UploadRecordVO> pageUploadRecords(Integer current, Integer size, String keyword);
    
    /**
     * 预览某批次前N行
     */
    java.util.List<String> previewUpload(String importBatch, Integer limit);
    
    /**
     * 更新批次元数据（国家/数据类型）
     */
    Boolean updateUploadMeta(String importBatch, String country, String dataType);
    
    /**
     * 批量删除上传批次
     */
    Boolean deleteUploadBatch(java.util.List<String> batches);
    
    /**
     * 批量添加基础数据
     */
    Integer batchAddBaseData(List<BaseData> dataList);
    
    /**
     * 删除基础数据
     */
    Boolean deleteBaseData(Long id);
    
    /**
     * 批量删除
     */
    Boolean batchDeleteBaseData(List<Long> ids);
}
