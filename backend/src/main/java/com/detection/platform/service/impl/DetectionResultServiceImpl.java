package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.detection.platform.config.GlobalExceptionHandler;
import com.detection.platform.dao.DetectionResultMapper;
import com.detection.platform.entity.DetectionResult;
import com.detection.platform.service.DetectionResultService;
import com.detection.platform.vo.DetectionResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 检测结果Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionResultServiceImpl extends ServiceImpl<DetectionResultMapper, DetectionResult> 
        implements DetectionResultService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public Page<DetectionResultVO> pageResults(Integer current, Integer size, Long taskId, Integer status,
                                               LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<DetectionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(taskId != null, DetectionResult::getTaskId, taskId);
        wrapper.eq(status != null, DetectionResult::getDetectStatus, status);
        wrapper.ge(startTime != null, DetectionResult::getDetectTime, startTime);
        wrapper.le(endTime != null, DetectionResult::getDetectTime, endTime);
        wrapper.orderByDesc(DetectionResult::getDetectTime);
        
        Page<DetectionResult> page = this.page(new Page<>(current, size), wrapper);
        
        Page<DetectionResultVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<DetectionResultVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }
    
    @Override
    public List<DetectionResultVO> listResultsByTaskId(Long taskId) {
        LambdaQueryWrapper<DetectionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DetectionResult::getTaskId, taskId);
        wrapper.orderByDesc(DetectionResult::getDetectTime);
        
        List<DetectionResult> list = this.list(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public byte[] exportResultsToExcel(Long taskId) throws IOException {
        if (taskId == null) {
            throw new GlobalExceptionHandler.BusinessException("任务ID不能为空");
        }
        
        // 查询检测结果
        LambdaQueryWrapper<DetectionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DetectionResult::getTaskId, taskId);
        wrapper.orderByDesc(DetectionResult::getDetectTime);
        List<DetectionResult> results = this.list(wrapper);
        
        if (results.isEmpty()) {
            throw new GlobalExceptionHandler.BusinessException("没有可导出的数据");
        }
        
        // 创建Excel工作簿
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("检测结果");
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "数据值", "检测状态", "响应时间(ms)", "检测时间", "错误信息"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }
            
            // 填充数据
            int rowNum = 1;
            for (DetectionResult result : results) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(result.getAccountIdentifier());
                row.createCell(2).setCellValue(getStatusText(result.getDetectStatus()));
                row.createCell(3).setCellValue(result.getResponseTime() != null ? result.getResponseTime() : 0);
                row.createCell(4).setCellValue(result.getDetectTime() != null ? 
                        result.getDetectTime().format(DATE_FORMATTER) : "");
                row.createCell(5).setCellValue(result.getErrorMsg() != null ? result.getErrorMsg() : "");
            }
            
            // 写入输出流
            workbook.write(out);
            
            log.info("导出检测结果成功, 任务ID: {}, 数据量: {}", taskId, results.size());
            return out.toByteArray();
        }
    }
    
    @Override
    public DetectionResultVO getTaskStatistics(Long taskId) {
        DetectionResultVO vo = new DetectionResultVO();
        
        if (taskId == null) {
            // 全局统计 - 统计所有任务的结果
            long totalCount = this.count();
            
            LambdaQueryWrapper<DetectionResult> successWrapper = new LambdaQueryWrapper<>();
            successWrapper.eq(DetectionResult::getDetectStatus, 1); // 状态1表示已注册/成功
            long successCount = this.count(successWrapper);
            
            long failCount = totalCount - successCount;
            
            vo.setTotalCount((int) totalCount);
            vo.setSuccessCount((int) successCount);
            vo.setFailCount((int) failCount);
            
            log.info("全局统计完成, 总数: {}, 成功: {}, 失败: {}", totalCount, successCount, failCount);
        } else {
            // 统计指定任务的结果
            LambdaQueryWrapper<DetectionResult> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.eq(DetectionResult::getTaskId, taskId);
            long totalCount = this.count(totalWrapper);
            
            // 统计成功数
            LambdaQueryWrapper<DetectionResult> successWrapper = new LambdaQueryWrapper<>();
            successWrapper.eq(DetectionResult::getTaskId, taskId);
            successWrapper.eq(DetectionResult::getDetectStatus, 1); // 状态1表示已注册/成功
            long successCount = this.count(successWrapper);
            
            // 统计失败数
            long failCount = totalCount - successCount;
            
            vo.setTaskId(taskId);
            vo.setTotalCount((int) totalCount);
            vo.setSuccessCount((int) successCount);
            vo.setFailCount((int) failCount);
            
            log.info("任务统计完成, 任务ID: {}, 总数: {}, 成功: {}, 失败: {}", 
                    taskId, totalCount, successCount, failCount);
        }
        
        return vo;
    }
    
    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 1 -> "成功";
            case 2 -> "失败";
            case 3 -> "超时";
            default -> "未知";
        };
    }
    
    /**
     * 实体转VO
     */
    private DetectionResultVO convertToVO(DetectionResult result) {
        DetectionResultVO vo = new DetectionResultVO();
        BeanUtils.copyProperties(result, vo);
        
        // 设置状态文本
        vo.setStatusText(getStatusText(result.getDetectStatus()));
        
        return vo;
    }
}
