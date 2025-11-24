package com.detection.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.common.utils.Result;
import com.detection.platform.service.BaseDataService;
import com.detection.platform.service.DetectionResultService;
import com.detection.platform.vo.BaseDataVO;
import com.detection.platform.vo.DetectionResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据中心Controller
 */
@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
public class DataController {
    
    private final BaseDataService baseDataService;
    private final DetectionResultService detectionResultService;
    
    // ==================== 基础数据管理 ====================
    
    /**
     * 分页查询基础数据
     */
    @GetMapping("/base/page")
    public Result<Page<BaseDataVO>> pageBaseData(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer dataType) {
        Page<BaseDataVO> page = baseDataService.pageBaseData(current, size, keyword, dataType);
        return Result.success(page);
    }
    
    /**
     * 上传TXT文本数据
     * @param file TXT文件，每行一个纯数字号码
     * @param country 国家
     * @param dataType 数据类型
     */
    @PostMapping("/base/import")
    public Result<Integer> importTxt(
            @RequestParam("file") MultipartFile file,
            @RequestParam("country") String country,
            @RequestParam("dataType") String dataType) throws IOException {
        Integer count = baseDataService.importTxt(file, country, dataType);
        return Result.success("导入成功,共" + count + "条数据", count);
    }
    
    /**
     * 删除基础数据
     */
    @DeleteMapping("/base/{id}")
    public Result<Void> deleteBaseData(@PathVariable Long id) {
        baseDataService.deleteBaseData(id);
        return Result.successMsg("删除成功");
    }
    
    /**
     * 上传记录分页
     */
    @GetMapping("/base/upload/page")
    public Result<Page<com.detection.platform.vo.UploadRecordVO>> pageUploadRecords(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<com.detection.platform.vo.UploadRecordVO> page = baseDataService.pageUploadRecords(current, size, keyword);
        return Result.success(page);
    }

    /**
     * 预览批次前10行
     */
    @GetMapping("/base/upload/preview")
    public Result<java.util.List<String>> previewUpload(@RequestParam("batch") String batch,
                                                        @RequestParam(required = false, defaultValue = "10") Integer limit) {
        java.util.List<String> lines = baseDataService.previewUpload(batch, limit);
        return Result.success(lines);
    }

    /**
     * 更新批次元数据
     */
    @PostMapping("/base/upload/update")
    public Result<Boolean> updateUploadMeta(@RequestParam("batch") String batch,
                                            @RequestParam(required = false) String country,
                                            @RequestParam(required = false) String dataType) {
        Boolean ok = baseDataService.updateUploadMeta(batch, country, dataType);
        return ok ? Result.success(true) : Result.error("更新失败");
    }

    /**
     * 批量删除上传批次
     */
    @DeleteMapping("/base/upload/batch")
    public Result<Boolean> batchDeleteUploads(@RequestBody java.util.List<String> batches) {
        Boolean ok = baseDataService.deleteUploadBatch(batches);
        return ok ? Result.success(true) : Result.error("删除失败");
    }
    
    // ==================== 检测结果管理 ====================
    
    /**
     * 分页查询检测结果
     */
    @GetMapping("/result/page")
    public Result<Page<DetectionResultVO>> pageResults(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        Page<DetectionResultVO> page = detectionResultService.pageResults(current, size, taskId, status, startTime, endTime);
        return Result.success(page);
    }
    
    /**
     * 根据任务ID查询结果列表
     */
    @GetMapping("/result/list")
    public Result<List<DetectionResultVO>> listResultsByTaskId(@RequestParam Long taskId) {
        List<DetectionResultVO> list = detectionResultService.listResultsByTaskId(taskId);
        return Result.success(list);
    }
    
    /**
     * 导出检测结果为Excel
     */
    @GetMapping("/result/export")
    public ResponseEntity<byte[]> exportResults(@RequestParam Long taskId) throws IOException {
        byte[] excelData = detectionResultService.exportResultsToExcel(taskId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "detection_results_" + taskId + ".xlsx");
        
        return ResponseEntity.ok().headers(headers).body(excelData);
    }
    
    /**
     * 统计任务结果
     */
    @GetMapping("/result/statistics")
    public Result<DetectionResultVO> getTaskStatistics(@RequestParam(required = false) Long taskId) {
        DetectionResultVO statistics = detectionResultService.getTaskStatistics(taskId);
        return Result.success(statistics);
    }
}
