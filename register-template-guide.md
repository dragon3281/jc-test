# 通用执行器 + 站点级配置模板设计指南

本指南用于规范自动化注册功能的“通用执行器 + 站点级配置”模式，帮助你扩展到更多网站并保持可维护性、可复用性。

## 目标
- 用一个“通用执行器”处理所有站点的注册请求、加密与 token 提取
- 为每个站点维护一份“独立配置”，执行器按配置取用必要参数与规则
- 站点分析自动生成配置；新增站点无需写新逻辑，仅更新配置

## 架构概览
- 分析器：`SmartWebAnalyzer`（域名 → 注册配置）
- 执行器：`RegisterTaskServiceImpl`（加载域名配置 → 构造请求 → 加密 → 提交 → 提取 token）
- 控制器入口：`BusinessController`（启动分析与查询结果）
- 持久化：分析结果与派生模板（可选）记录在库中，供复用

## 注册配置（RegisterProfile）推荐字段
每个域名保存一条配置（示例采用 JSON 结构说明字段），执行器严格按该站点配置取用：

```json
{
  "domain": "ppvip4.com",
  "registerApi": "/wps/member/register",
  "method": "PUT",

  "encryption": {
    "type": "DES_RSA",
    "desKeySource": "ORIGINAL_RND_8B",        // 用原始 rnd 的前 8 字节作为 DES 密钥
    "rsaPayloadSource": "REVERSED_RND",        // RSA 加密使用反转后的 rnd
    "rsaKeyApi": "/wps/session/key/rsa",       // 未检测时的默认值
    "valueFieldName": "value",                 // 请求体密文字段名
    "encryptionHeader": "Encryption"           // 加密头名（未检测到时用此默认）
  },

  "headers": {
    "Device": "web",
    "Language": "BN",
    "MerchantRule": [
      { "match": "ppvip*", "value": "ppvipbdtf5" },
      { "match": "*",      "value": "ck555bdtf3" }
    ],
    "extra": { }
  },

  "cookies": {
    "default": [ { "name": "SHELL_deviceId", "valueGen": "UUID" } ],
    "extra": [ ]
  },

  "fields": {
    "map": {
      "username": "username",
      "password": "password",
      "confirmPassword": "confirmPassword"
    },
    "requiredCandidates": [
      "username","password","confirmPassword","email","mobileNum","captcha"
    ]
  },

  "response": {
    "tokenPriorities": [
      "value.token",
      "data.token",
      "token",
      "headers.authorization",
      "headers.*token*"
    ]
  }
}
```

说明与约束：
- 执行器不会把“所有候选字段”都发送，只按该站点的 `fields.map` 与密文 `valueFieldName` 构造必要请求体。
- `MerchantRule` 为简化表达：执行器按域名匹配规则套用 merchant 值。
- `extra` 允许站点级注入额外 headers/cookies（例如必须的 CSRF）。

## 两个站点的配置示例
根据当前分析结果，建议初始条目如下（可落库或转为模板）：

### ppvip4.com
```json
{
  "domain": "ppvip4.com",
  "registerApi": "/wps/member/register",
  "method": "PUT",
  "encryption": {
    "type": "DES_RSA",
    "desKeySource": "ORIGINAL_RND_8B",
    "rsaPayloadSource": "REVERSED_RND",
    "rsaKeyApi": "/wps/session/key/rsa",
    "valueFieldName": "value",
    "encryptionHeader": "Encryption"
  },
  "headers": {
    "Device": "web",
    "Language": "BN",
    "MerchantRule": [ { "match": "ppvip*", "value": "ppvipbdtf5" }, { "match": "*", "value": "ck555bdtf3" } ],
    "extra": {}
  },
  "cookies": { "default": [ { "name": "SHELL_deviceId", "valueGen": "UUID" } ], "extra": [] },
  "fields": {
    "map": { "username": "username", "password": "password", "confirmPassword": "confirmPassword" },
    "requiredCandidates": ["username","password","confirmPassword","email","mobileNum","captcha"]
  },
  "response": { "tokenPriorities": ["value.token","data.token","token","headers.authorization","headers.*token*"] }
}
```

### wwwtk666.com
```json
{
  "domain": "wwwtk666.com",
  "registerApi": "/wps/member/register",
  "method": "PUT",
  "encryption": {
    "type": "DES_RSA",
    "desKeySource": "ORIGINAL_RND_8B",
    "rsaPayloadSource": "REVERSED_RND",
    "rsaKeyApi": "/wps/session/key/rsa",
    "valueFieldName": "value",
    "encryptionHeader": "Encryption"
  },
  "headers": {
    "Device": "web",
    "Language": "BN",
    "MerchantRule": [ { "match": "ppvip*", "value": "ppvipbdtf5" }, { "match": "*", "value": "ck555bdtf3" } ],
    "extra": {}
  },
  "cookies": { "default": [ { "name": "SHELL_deviceId", "valueGen": "UUID" } ], "extra": [] },
  "fields": {
    "map": { "username": "username", "password": "password", "confirmPassword": "confirmPassword" },
    "requiredCandidates": ["username","password","confirmPassword"]
  },
  "response": { "tokenPriorities": ["value.token","data.token","token","headers.authorization","headers.*token*"] }
}
```

## 模板制作与落地流程
1. 站点分析
   - 运行 `SmartWebAnalyzer`，自动识别接口、方法、加密模式与 RSA 公钥接口，生成分析结果
   - 将分析结果规范化为“注册配置”（必要字段＋默认值）

2. 模板固化（可选但推荐）
   - 从分析结果派生“注册模板”，仅保留执行所需字段并消除不稳定项（如临时候选）
   - 为每模板赋版本号与站点归属（domain），用于回滚与审计

3. 执行器读取与执行
   - `RegisterTaskServiceImpl` 按域名加载注册配置/模板
   - 构造请求头（`Device`、`Language`、`Merchant` 根据规则），生成 cookie（默认 `SHELL_deviceId`）
   - 构造请求体：明文参数映射 + 密文 `valueFieldName`
   - 加密：
     - DES：生成 16 位 `rnd`，取“原始 rnd 的前 8 字节”为密钥（ECB + PKCS7）
     - RSA：对“反转后的 rnd”执行 JS-RSA 加密（公钥来自 `rsaKeyApi`）
     - 在请求头中添加 `encryptionHeader`: RSA 密文
   - 发送请求并按 `response.tokenPriorities` 提取 token

4. 更新与复用
   - 保存执行成功的结果（含 token 与响应摘要），用于后续验证与回归
   - 若站点规则变化，仅需更新该站点的配置/模板，无需改通用执行器

## 关键实现要点（执行器）
- DES 与 RSA 的源：
  - DES 密钥：原始 `rnd` 的前 8 字节
  - RSA 内容：反转后的 `rnd`（`split('') → reverse() → join('')`）
- 请求头：统一为 `Device=web`、`Language=BN`，`Merchant` 由域名匹配规则生成
- 加密头名：默认 `Encryption`，若检测到站点自定义则用站点值
- Cookie：默认生成 `SHELL_deviceId=<UUID>`；允许注入额外 cookies
- Token 提取优先级：依次尝试 `value.token → data.token → token → headers`

## 表结构建议（示意）
- t_website_analysis：保存分析结果（原始内容与摘要）
- t_register_profile：规范化后的注册配置（域名唯一）
- t_register_template：从配置派生的可执行模板（版本化，可回滚）（可选）

## 命名与规范
- 字段映射仅保留执行所需的 canonical → actual 映射；避免“一锅炖”的大集合
- 规则命名明确：`desKeySource=ORIGINAL_RND_8B`、`rsaPayloadSource=REVERSED_RND`
- Merchant 规则应支持 domainPattern → value 的多条匹配；未命中走默认

## 测试清单（新增/变更站点）
- 能拉到 RSA 公钥（`rsaKeyApi`）
- DES 密文长度与前缀可打印校验
- RSA 密文长度应为 256（典型）
- 请求头包含 `Device`、`Language`、`Merchant` 与 `Encryption`
- 响应成功且能从优先队列规则中提取到 token

## 常见问题与排查
- RSA 加密失败：检查 Python JS 执行依赖（PyExecJS），公钥是否为空或格式异常
- 服务器返回 `decryption.err`：检查 DES 密钥是否取“原始 rnd 的前 8 字节”、模式是否 ECB + PKCS7
- 响应成功无 token：调整 `response.tokenPriorities`，并检查是否为 header 返回

## 与现有代码的对应关系
- 分析器：`SmartWebAnalyzer` 负责生成/更新站点分析与细节（接口、加密、头、字段等）
- 执行器：`RegisterTaskServiceImpl` 按站点配置执行注册并提取 token
- 控制器：`BusinessController` 提供启动分析与读取结果的 API

---
如需，我可以将上面两个站点的配置直接落库或导出为可导入的 JSON；也可以把现有分析结果自动转存为 `t_register_profile` 初始条目，方便你统一管理与版本化。
