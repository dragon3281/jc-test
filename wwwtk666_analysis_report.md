# https://www.wwwtk666.com 网站分析报告

## 执行时间
2025-11-26 17:58

## 网站信息
- **网站URL**: https://www.wwwtk666.com
- **网站类型**: 博彩游戏平台（孟加拉国）
- **前端框架**: Vue.js (单页应用)
- **服务器**: Cloudflare CDN

## 🔍 智能分析结果

### 1. 注册页面
- **PC端注册页**: /register
- **移动端注册页**: /m/register
- **自动跳转逻辑**: 检测设备类型自动跳转到对应页面

### 2. 加密方式识别

#### ✅ 已确认：DES + RSA 组合加密

**证据来源 - `/js/encrypt.js?v=9215`**:

```javascript
// 1. 使用CryptoJS库进行加密
var CryptoJS=CryptoJS||function(_0x566a04,_0x34d35b){...

// 2. RSA加密函数
function rsaEncrypt(_0x294ac6,_0x3bde93){
    var _0x3c1b8c=getPulicRsa();
    if(null!=_0x3c1b8c)
        return setMaxDigits(0x82),
               encryptedString(new RSAKeyPair('10001','',_0x3c1b8c),_0x294ac6);
}

// 3. DES加密函数
CryptoJS['DES']['encrypt']({
    'ciphertext':CryptoJS['enc']['Base64']['parse'](_0x3032bd)
}, _0x52e796, {
    'mode':CryptoJS['mode']['ECB'],
    'padding':CryptoJS['pad']['Pkcs7']
})

// 4. 组合加密函数 reRsa
var reRsa=function(_0x32d30d,_0x1bf929){
    var _0x16e504=rndString(),  // 生成随机字符串
    var _0x255e6a=rsaEncrypt(_0x16e504['split']('')['reverse']()['join'](''));
    
    // DES加密数据
    var _0x4bb936=CryptoJS['enc']['Utf8']['parse'](_0x16e504),
    var _0x5cd66f=CryptoJS['DES']['encrypt'](_0x1d262c,_0x4bb936,{
        'mode':CryptoJS['mode']['ECB'],
        'padding':CryptoJS['pad']['Pkcs7']
    });
    
    // 返回加密结果
    _0x2baae6['RSA']=_0x255e6a,
    _0x2baae6['DES']=_0x5cd66f['toString']()
}
```

**加密流程**:
1. 生成16位随机字符串作为DES密钥
2. 将随机字符串反转后用RSA加密
3. 用随机字符串作为DES密钥加密实际数据
4. 返回 `{RSA: <加密后的密钥>, DES: <加密后的数据>}`

### 3. RSA公钥获取

**接口**: `/wps/session/key/rsa?t={timestamp}`
**方法**: GET
**请求头**: 
```
Cache-Control: no-cache
```

**关键代码**:
```javascript
function getSend(){
    var _0x3cbfba='/wps/session/key/rsa?t='+new Date().getTime();
    objXMLHttp.open('GET',_0x3cbfba,!0x1),
    objXMLHttp.setRequestHeader('Cache-Control','no-cache'),
    objXMLHttp.onreadystatechange=processResponse,
    objXMLHttp.send(null);
}

function processResponse(){
    if(0x4==objXMLHttp.readyState){
        if(0xc8==objXMLHttp.status)
            publicRsa=objXMLHttp.responseText;
    }
}
```

### 4. 注册参数分析

#### 从 localStorage 获取推荐码
```javascript
keepRegInfo(){
    var e=new URLSearchParams(window.location.search),
    i=e.get("affiliateCode"),
    t=e.get("r")||e.get("referralCode"),
    o={};
    i&&(o.affiliateCode=i),
    t&&(o.referralCode=t),
    Object.keys(o).length>0&&
        window.localStorage.setItem("reg_info",JSON.stringify(o))
}
```

#### 可能的注册字段
- `username` - 用户名
- `password` - 密码（需要DES+RSA加密）
- `email` - 邮箱（可选）
- `phone` - 手机号
- `affiliateCode` - 推广码（来自URL或localStorage）
- `referralCode` - 推荐码（来自URL参数 `r` 或 `referralCode`）
- `captcha` - 验证码

### 5. 注册接口推测

基于常见模式，注册接口可能是：
- `/wps/api/register`
- `/wps/user/register`
- `/wps/member/register`
- `/api/register`

**请求方法**: POST
**Content-Type**: application/json
**请求体格式**:
```json
{
  "RSA": "<RSA加密后的DES密钥>",
  "DES": "<DES加密后的用户数据>"
}
```

其中DES加密前的原始数据可能是：
```json
{
  "username": "testuser",
  "password": "password123",
  "phone": "13812345678",
  "referralCode": "REFCODE",
  "captcha": "1234"
}
```

### 6. 加密参数详细说明

#### RSA参数
- **公钥指数**: `10001` (65537, 标准值)
- **公钥模数**: 从 `/wps/session/key/rsa` 接口动态获取
- **密钥长度**: 130位 (setMaxDigits(0x82) = 130)

#### DES参数
- **模式**: ECB (Electronic Codebook)
- **填充**: Pkcs7
- **密钥**: 16位随机字符串
- **字符集**: `0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz`

## 📊 实际测试建议

### 测试步骤

1. **获取RSA公钥**
   ```bash
   curl "https://www.wwwtk666.com/wps/session/key/rsa?t=$(date +%s)000" \
     -H "Cache-Control: no-cache"
   ```

2. **生成测试数据**
   - 用户名: test_1732617893456
   - 密码: Test@123456
   - 手机号: 13812345678

3. **执行加密**
   - 生成16位随机DES密钥，例如: `Kq2pLm7Nx1Zw4Js`
   - 反转密钥: `sJ4wZ1xN7mLp2qK`
   - 用RSA公钥加密反转后的密钥
   - 用原始密钥进行DES-ECB加密用户数据

4. **发送注册请求**
   ```bash
   curl -X POST "https://www.wwwtk666.com/wps/api/register" \
     -H "Content-Type: application/json" \
     -d '{
       "RSA": "<加密后的密钥>",
       "DES": "<加密后的数据>"
     }'
   ```

5. **分析响应**
   - 检查HTTP状态码
   - 从响应JSON中提取 `token` 字段
   - 可能的字段名: `token`, `accessToken`, `authToken`, `sessionId`

## ⚠️ 技术难点

1. **RSA加密实现**
   - 需要实现老式的 `RSAKeyPair` 和 `encryptedString` 函数
   - Java标准库的RSA可能与JS库不兼容

2. **验证码处理**
   - 注册可能需要图形验证码或短信验证码
   - 需要OCR识别或人工输入

3. **请求头和Cookie**
   - 可能需要携带特定的请求头
   - 可能需要先访问首页获取Session Cookie

4. **反爬虫机制**
   - Cloudflare可能有bot检测
   - 需要模拟真实浏览器行为

## 🎯 结论

### 已识别的信息
✅ 加密方式: **DES + RSA 组合加密**  
✅ RSA公钥获取接口: `/wps/session/key/rsa?t={timestamp}`  
✅ DES模式: ECB + Pkcs7  
✅ 密钥生成: 16位随机字符串（反转后RSA加密）  
✅ 注册页面: `/register` (PC) `/m/register` (移动端)  

### 未识别的信息
❌ 具体的注册接口URL  
❌ 完整的注册参数列表  
❌ 是否需要验证码  
❌ Token在响应中的确切字段名  

### 下一步建议

1. **优化智能分析器**
   - 实现Java版本的DES-ECB加密
   - 实现与JS兼容的RSA加密（老式 encryptedString）
   - 添加字符串反转逻辑

2. **手动测试**
   - 在浏览器开发者工具中监控注册请求
   - 记录完整的请求URL、headers、body
   - 验证加密参数的格式

3. **改进参数提取**
   - 下载完整的Vue应用代码
   - 反混淆JS代码
   - 查找注册表单的提交逻辑

## 📝 附加说明

- 该网站使用了代码混淆 (变量名如 `_0x23f4b8`, `a0_0x4c97`)
- JS文件带版本号 `?v=9215` 用于缓存控制
- 支持移动端和PC端两套UI
- 有推荐码/推广码系统
- 使用localStorage保存注册信息

## 🔐 加密方式总结

**类型**: DES_RSA (老式JS加密库)  
**加密库**: CryptoJS + 自定义RSA实现  
**复杂度**: ⭐⭐⭐⭐ (较复杂)  
**可破解性**: ✅ 可以，但需要实现兼容的加密逻辑  
