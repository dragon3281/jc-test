#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
对比Java和Python的加密差异
从后端日志中提取Java的加密参数，然后与Python对比
"""

import re

# 从日志中提取任务25的第一个用户的信息
log_file = "/root/jc-test/logs/backend.log"

print("=" * 80)
print("分析Java的加密流程（从后端日志提取）")
print("=" * 80)

with open(log_file, 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

# 提取关键信息
rnd = None
reversed_rnd = None
rsa_public_key = None
username = None
plaintext_json = None
des_encrypted = None
rsa_encrypted = None

in_register_25 = False
for i, line in enumerate(lines):
    if "[Register-25]" in line:
        in_register_25 = True
        
        # 提取rnd
        if "原始随机字符串 rnd:" in line:
            match = re.search(r'rnd: (\w+)', line)
            if match:
                rnd = match.group(1)
                
        # 提取reversed_rnd  
        if "反转后 reversedRnd:" in line:
            match = re.search(r'reversedRnd: (\w+)', line)
            if match:
                reversed_rnd = match.group(1)
                
        # 提取用户名（从任何包含用户名的行）
        if "username=" in line or '"username":' in line:
            # 先尝试从Map内容提取
            match = re.search(r'username=(\d+)', line)
            if match and username is None:
                username = match.group(1)
            # 再尝试从JSON提取
            match2 = re.search(r'"username":"(\d+)"', line)
            if match2 and username is None:
                username = match2.group(1)
                
        # 提取明文JSON
        if "明文参数(无空格):" in line:
            match = re.search(r'明文参数\(无空格\): (.+)$', line)
            if match:
                plaintext_json = match.group(1).strip()
                
        # 提取RSA公钥
        if "获取到RSA公钥:" in line:
            match = re.search(r'获取到RSA公钥: (.+)$', line)
            if match:
                rsa_public_key = match.group(1).strip()
                
        # 提取DES密文前80字符
        if "DES密文前80字符:" in line:
            match = re.search(r'DES密文前80字符: (.+)$', line)
            if match:
                des_encrypted_prefix = match.group(1).strip()
                
        # 提取RSA密文完整
        if "RSA密文完整:" in line:
            match = re.search(r'RSA密文完整: (.+)$', line)
            if match:
                rsa_encrypted = match.group(1).strip()
                break  # 第一个用户的信息已收集完毕

print(f"\n【Java加密参数（从日志提取）】")
print(f"1. 原始rnd: {rnd}")
print(f"2. 反转rnd: {reversed_rnd}")
print(f"3. 用户名: {username}")
print(f"4. RSA公钥前80字符: {rsa_public_key[:80] if rsa_public_key else 'N/A'}")
print(f"5. RSA公钥长度: {len(rsa_public_key) if rsa_public_key else 0}")
print(f"6. 明文JSON长度: {len(plaintext_json) if plaintext_json else 0}")
print(f"7. RSA密文长度: {len(rsa_encrypted) if rsa_encrypted else 0}")
print(f"8. RSA密文前120字符: {rsa_encrypted[:120] if rsa_encrypted else 'N/A'}")

print("\n" + "=" * 80)
print("使用Python重新加密相同的参数，验证一致性")
print("=" * 80)

if all([rnd, reversed_rnd, username, rsa_public_key, plaintext_json]):
    import execjs
    from pathlib import Path
    
    script_dir = Path(__file__).parent
    
    # Python DES加密
    with open(script_dir / 'des加解密.js', 'r', encoding='utf-8') as f:
        js_code = f.read()
    ctx_des = execjs.compile(js_code)
    python_des_encrypted = ctx_des.call('DES_Encrypt', plaintext_json, reversed_rnd)
    
    # Python RSA加密
    with open(script_dir / '加密逻辑.js', 'r', encoding='utf-8') as f:
        key_code = f.read()
    ctx_rsa = execjs.compile(key_code)
    python_rsa_encrypted = ctx_rsa.call('rsaEncryptV2', rsa_public_key, rnd)
    
    print(f"\n【Python加密结果】")
    print(f"1. DES密文前80字符: {python_des_encrypted[:80]}")
    print(f"2. DES密文长度: {len(python_des_encrypted)}")
    print(f"3. RSA密文前120字符: {python_rsa_encrypted[:120]}")
    print(f"4. RSA密文长度: {len(python_rsa_encrypted)}")
    
    print("\n" + "=" * 80)
    print("对比结果")
    print("=" * 80)
    
    if des_encrypted_prefix:
        des_match = python_des_encrypted.startswith(des_encrypted_prefix)
        print(f"✅ DES加密一致: {des_match}" if des_match else f"❌ DES加密不一致")
        
    rsa_match = python_rsa_encrypted == rsa_encrypted
    print(f"{'✅' if rsa_match else '❌'} RSA加密一致: {rsa_match}")
    
    if not rsa_match and rsa_encrypted:
        print(f"\n差异分析：")
        print(f"Java RSA:   {rsa_encrypted[:120]}...")
        print(f"Python RSA: {python_rsa_encrypted[:120]}...")
        print(f"\nJava RSA长度: {len(rsa_encrypted)}")
        print(f"Python RSA长度: {len(python_rsa_encrypted)}")
        
        # 逐字符对比
        for idx in range(min(len(rsa_encrypted), len(python_rsa_encrypted))):
            if rsa_encrypted[idx] != python_rsa_encrypted[idx]:
                print(f"\n❌ 第{idx}个字符不同:")
                print(f"   Java:   '{rsa_encrypted[max(0,idx-10):idx+11]}'")
                print(f"   Python: '{python_rsa_encrypted[max(0,idx-10):idx+11]}'")
                break
                
    print("\n" + "=" * 80)
    print("结论")
    print("=" * 80)
    
    if rsa_match and des_encrypted_prefix and des_match:
        print("✅ Java和Python的加密结果完全一致！")
        print("   问题可能出在HTTP请求的其他方面（请求头、时序等）")
    else:
        print("❌ 加密结果不一致，需要检查加密逻辑！")
        
else:
    print("\n❌ 无法从日志中提取完整信息")
    print(f"   rnd: {rnd}")
    print(f"   reversed_rnd: {reversed_rnd}")
    print(f"   username: {username}")
    print(f"   rsa_public_key: {'已提取' if rsa_public_key else '未提取'}")
    print(f"   plaintext_json: {'已提取' if plaintext_json else '未提取'}")
