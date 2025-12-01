# 用户名长度问题修复方案

## 问题描述
SmartWebAnalyzer生成的测试用户名为15位（"test_" + 11位数字），
但网站要求用户名长度为6-10位，导致注册测试失败。

错误信息：`"errorCode":"request.param.err.length.username"`

## 已完成的工作
1. ✅ 创建了UsernamePasswordRuleDetector类（智能规则检测）
2. ✅ 创建了UsernameGenerator类（生成8位用户名）  
3. ✅ 两个类已编译并添加到JAR包

## 剩余问题
SmartWebAnalyzer.java第781行仍使用旧逻辑：
```java
String testUsername = "test_" + timestamp % 100000000000L;  // 生成15位
```

需要改为：
```java
String testUsername = UsernameGenerator.generate(8);  // 生成8位
```

## 解决方案

### 方案1：字节码替换（推荐，无需重启）
使用字节码操作工具（如javassist）在运行时替换方法

### 方案2：重新编译（需要解决Maven问题）
1. 修复Maven编译卡住的问题
2. 修改源代码
3. 重新编译打包
4. 重启服务

### 方案3：手动修改class文件
1. 反编译SmartWebAnalyzer.class
2. 修改第781行代码
3. 重新编译
4. 替换JAR中的class文件
5. 重启服务

## 当前建议
由于Maven编译存在问题，建议：
1. 先使用方案3快速修复
2. 后续优化时解决Maven问题，使用方案2完整重新编译

