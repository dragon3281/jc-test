#!/bin/bash
# 测试Java调用Python加密服务的集成

echo "========== 测试Python加密服务 =========="

# 测试RSA加密
echo -e "\n【测试1】RSA加密："
python3 /root/jc-test/test/encryption_service.py rsa_encrypt \
  "ba996bee3c03e9e80a1ed348dd0cc93c579c8e86992a12d39e210b99107a3835c2e53f254c6ffba4fd761dd61349b768f9f5bab22e3083beb8a76edcacbe2d2381975b5bbb04848c9e960c0dcc03652e0fa0da7d86946276e7c3f735e3c03937dfb403eeccec995a366598669add3d358c9ebce9e8ca0a2a332694b31612ff85" \
  "JJg43ze3KrqE6kq7"
echo "Exit code: $?"

# 测试DES加密
echo -e "\n【测试2】DES加密："
python3 /root/jc-test/test/encryption_service.py des_encrypt \
  '{"username":"test123","password":"Pass123"}' \
  "Sg3soqia"
echo "Exit code: $?"

# 测试execjs是否正常
echo -e "\n【测试3】检查execjs模块："
python3 -c "import execjs; print('execjs版本:', execjs.__version__); print('JS引擎:', execjs.get().name)"

# 测试文件存在性
echo -e "\n【测试4】检查JS文件："
ls -lh /root/jc-test/test/加密逻辑.js
ls -lh /root/jc-test/test/des加解密.js

# 测试JS文件加载
echo -e "\n【测试5】测试JS文件是否能被正确加载："
python3 <<'EOF'
import execjs
from pathlib import Path

script_dir = Path('/root/jc-test/test')

# 测试加载加密逻辑.js
try:
    with open(script_dir / '加密逻辑.js', 'r', encoding='utf-8') as f:
        js_code = f.read()
    ctx = execjs.compile(js_code)
    print("✅ 加密逻辑.js 加载成功")
    print(f"   文件大小: {len(js_code)} 字节")
except Exception as e:
    print(f"❌ 加密逻辑.js 加载失败: {e}")

# 测试加载des加解密.js
try:
    with open(script_dir / 'des加解密.js', 'r', encoding='utf-8') as f:
        js_code = f.read()
    ctx = execjs.compile(js_code)
    print("✅ des加解密.js 加载成功")
    print(f"   文件大小: {len(js_code)} 字节")
except Exception as e:
    print(f"❌ des加解密.js 加载失败: {e}")
EOF

echo -e "\n========== 测试完成 =========="
