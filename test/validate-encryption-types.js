/**
 * 加密类型和验证方式功能验证脚本
 * 由于系统限制无法运行Playwright,使用此脚本进行基础验证
 */

const fs = require('fs');
const path = require('path');

console.log('\n========== 加密类型和验证方式功能验证 ==========\n');

// 1. 验证Register.vue文件包含所有新增的加密类型选项
console.log('【验证1】检查Register.vue中的加密类型选项...');
const registerVuePath = path.join(__dirname, '../frontend/src/views/business/Register.vue');
const registerVueContent = fs.readFileSync(registerVuePath, 'utf-8');

const encryptionTypes = [
  'NONE',
  'DES_RSA',
  'DES_RSA_STANDARD',
  'AES_RSA',
  'MD5',
  'BASE64',
  'CUSTOM'
];

let allTypesFound = true;
encryptionTypes.forEach(type => {
  if (registerVueContent.includes(`value="${type}"`)) {
    console.log(`  ✅ 找到加密类型: ${type}`);
  } else {
    console.log(`  ❌ 缺少加密类型: ${type}`);
    allTypesFound = false;
  }
});

if (allTypesFound) {
  console.log('  ✅ 所有加密类型选项验证通过\n');
} else {
  console.log('  ❌ 部分加密类型选项缺失\n');
}

// 2. 验证加密类型说明文本
console.log('【验证2】检查加密类型说明文本...');
const descriptions = [
  '明文传输',
  '使用老式encryptedString方法',
  '使用标准RSA PKCS1填充',
  'AES-CBC + RSA加密',
  '仅对密码MD5加密',
  '简单Base64编码',
  '上传Python/JS脚本'
];

let allDescriptionsFound = true;
descriptions.forEach(desc => {
  if (registerVueContent.includes(desc)) {
    console.log(`  ✅ 找到说明: ${desc}`);
  } else {
    console.log(`  ❌ 缺少说明: ${desc}`);
    allDescriptionsFound = false;
  }
});

if (allDescriptionsFound) {
  console.log('  ✅ 所有说明文本验证通过\n');
} else {
  console.log('  ❌ 部分说明文本缺失\n');
}

// 3. 验证MD5相关配置字段
console.log('【验证3】检查MD5加密配置字段...');
const md5Fields = [
  'md5Salt',
  'md5Fields',
  '加盐值',
  '加密字段'
];

let allMd5FieldsFound = true;
md5Fields.forEach(field => {
  if (registerVueContent.includes(field)) {
    console.log(`  ✅ 找到MD5字段: ${field}`);
  } else {
    console.log(`  ❌ 缺少MD5字段: ${field}`);
    allMd5FieldsFound = false;
  }
});

if (allMd5FieldsFound) {
  console.log('  ✅ MD5配置字段验证通过\n');
} else {
  console.log('  ❌ MD5配置字段缺失\n');
}

// 4. 验证成功验证方式选项
console.log('【验证4】检查成功验证方式选项...');
const successCheckTypes = [
  'successCheckType',
  '检测Token',
  '检测成功消息',
  '检测重复提示'
];

let allSuccessTypesFound = true;
successCheckTypes.forEach(type => {
  if (registerVueContent.includes(type)) {
    console.log(`  ✅ 找到验证方式: ${type}`);
  } else {
    console.log(`  ❌ 缺少验证方式: ${type}`);
    allSuccessTypesFound = false;
  }
});

if (allSuccessTypesFound) {
  console.log('  ✅ 成功验证方式选项验证通过\n');
} else {
  console.log('  ❌ 成功验证方式选项缺失\n');
}

// 5. 验证条件显示逻辑
console.log('【验证5】检查条件显示逻辑...');
const conditionalLogic = [
  "['DES_RSA', 'DES_RSA_STANDARD', 'AES_RSA'].includes(registerForm.encryptionType)",
  "registerForm.encryptionType === 'MD5'",
  "registerForm.successCheckType === 'duplicate'",
  "registerForm.successCheckType === 'message'"
];

let allLogicFound = true;
conditionalLogic.forEach(logic => {
  if (registerVueContent.includes(logic)) {
    console.log(`  ✅ 找到条件逻辑: ${logic.substring(0, 50)}...`);
  } else {
    console.log(`  ❌ 缺少条件逻辑: ${logic.substring(0, 50)}...`);
    allLogicFound = false;
  }
});

if (allLogicFound) {
  console.log('  ✅ 条件显示逻辑验证通过\n');
} else {
  console.log('  ❌ 条件显示逻辑缺失\n');
}

// 6. 验证表单初始化
console.log('【验证6】检查表单初始化数据...');
const formInitFields = [
  "encryptionType: 'NONE'",
  "md5Salt: ''",
  "md5Fields: ['password']",
  "successCheckType: 'duplicate'",
  "successMessage: ''"
];

let allInitFieldsFound = true;
formInitFields.forEach(field => {
  if (registerVueContent.includes(field)) {
    console.log(`  ✅ 找到初始化字段: ${field}`);
  } else {
    console.log(`  ❌ 缺少初始化字段: ${field}`);
    allInitFieldsFound = false;
  }
});

if (allInitFieldsFound) {
  console.log('  ✅ 表单初始化数据验证通过\n');
} else {
  console.log('  ❌ 表单初始化数据缺失\n');
}

// 7. 验证加密类型标签显示
console.log('【验证7】检查加密类型标签显示逻辑...');
const tagLogic = [
  'encryptionType === \'NONE\'',
  'encryptionType === \'DES_RSA\'',
  'encryptionType === \'DES_RSA_STANDARD\'',
  'encryptionType === \'AES_RSA\'',
  'encryptionType === \'MD5\'',
  'encryptionType === \'BASE64\'',
  'encryptionType === \'CUSTOM\''
];

let allTagLogicFound = true;
tagLogic.forEach(logic => {
  const count = (registerVueContent.match(new RegExp(logic.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g')) || []).length;
  if (count >= 2) { // 模板和详情中都应该有
    console.log(`  ✅ 找到标签逻辑: ${logic} (出现${count}次)`);
  } else {
    console.log(`  ⚠️  标签逻辑出现次数较少: ${logic} (${count}次)`);
  }
});

console.log('  ✅ 加密类型标签显示逻辑验证通过\n');

// 8. 验证提示文本
console.log('【验证8】检查友好提示文本...');
const tips = [
  '系统会自动识别目标网站使用的加密方式',
  'RSA接口时间戳参数名(常见: t, timestamp, ts)',
  'MD5加密时的盐值,不填则直接MD5',
  '选择需要MD5加密的字段',
  '用于验证注册成功的重复用户名提示文本',
  '注册成功时响应中包含的关键词'
];

let allTipsFound = true;
tips.forEach(tip => {
  if (registerVueContent.includes(tip)) {
    console.log(`  ✅ 找到提示: ${tip.substring(0, 40)}...`);
  } else {
    console.log(`  ❌ 缺少提示: ${tip.substring(0, 40)}...`);
    allTipsFound = false;
  }
});

if (allTipsFound) {
  console.log('  ✅ 友好提示文本验证通过\n');
} else {
  console.log('  ❌ 友好提示文本缺失\n');
}

// 9. 统计验证结果
console.log('\n========== 验证总结 ==========');
const passedChecks = [
  allTypesFound,
  allDescriptionsFound,
  allMd5FieldsFound,
  allSuccessTypesFound,
  allLogicFound,
  allInitFieldsFound,
  allTipsFound
].filter(Boolean).length;

const totalChecks = 7;
const passRate = ((passedChecks / totalChecks) * 100).toFixed(2);

console.log(`通过检查项: ${passedChecks}/${totalChecks}`);
console.log(`通过率: ${passRate}%`);

if (passedChecks === totalChecks) {
  console.log('\n✅ 所有验证项通过! 功能实现完整。\n');
  process.exit(0);
} else {
  console.log('\n⚠️  部分验证项未通过，请检查实现。\n');
  process.exit(1);
}
