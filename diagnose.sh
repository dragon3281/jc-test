#!/bin/bash

# 系统诊断脚本
# 输出文件：/root/jc-test/diagnose-report.txt

REPORT="/root/jc-test/diagnose-report.txt"

echo "========================================" > $REPORT
echo "系统诊断报告" >> $REPORT
echo "生成时间: $(date '+%Y-%m-%d %H:%M:%S')" >> $REPORT
echo "========================================" >> $REPORT
echo "" >> $REPORT

echo "1. Docker服务状态" >> $REPORT
echo "----------------------------------------" >> $REPORT
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" >> $REPORT 2>&1
echo "" >> $REPORT

echo "2. 端口占用情况" >> $REPORT
echo "----------------------------------------" >> $REPORT
netstat -tlnp | grep -E ":(3000|8080|3306|6379|5672)" >> $REPORT 2>&1
echo "" >> $REPORT

echo "3. Java进程" >> $REPORT
echo "----------------------------------------" >> $REPORT
ps aux | grep java | grep -v grep >> $REPORT 2>&1
echo "" >> $REPORT

echo "4. Node进程" >> $REPORT
echo "----------------------------------------" >> $REPORT
ps aux | grep node | grep -v grep >> $REPORT 2>&1
echo "" >> $REPORT

echo "5. 测试前端访问(3000端口)" >> $REPORT
echo "----------------------------------------" >> $REPORT
timeout 2 curl -I http://127.0.0.1:3000 >> $REPORT 2>&1
echo "退出码: $?" >> $REPORT
echo "" >> $REPORT

echo "6. 测试后端API(8080端口)" >> $REPORT
echo "----------------------------------------" >> $REPORT
timeout 2 curl -I http://127.0.0.1:8080/user/test-password?password=test >> $REPORT 2>&1
echo "退出码: $?" >> $REPORT
echo "" >> $REPORT

echo "7. 检查日志文件" >> $REPORT
echo "----------------------------------------" >> $REPORT
find /root/jc-test -name "*.log" -type f 2>/dev/null >> $REPORT
echo "" >> $REPORT

echo "8. 后端日志(最后20行)" >> $REPORT
echo "----------------------------------------" >> $REPORT
if [ -f "/root/jc-test/logs/detection-platform.log" ]; then
  tail -20 /root/jc-test/logs/detection-platform.log >> $REPORT 2>&1
else
  echo "后端日志文件不存在" >> $REPORT
fi
echo "" >> $REPORT

echo "========================================" >> $REPORT
echo "诊断完成" >> $REPORT
echo "========================================" >> $REPORT

chmod 644 $REPORT
echo "诊断报告已生成: $REPORT"
