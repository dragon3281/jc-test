#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
使用Java生成的RSA密文测试服务器是否能解密
从日志中提取Java的RSA密文和DES密文，直接发送给服务器
"""

import requests
import json
import urllib3
import re

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# 从日志中提取任务25第一个用户的加密数据
log_file = "/root/jc-test/logs/backend.log"

rsa_encrypted = None
des_encrypted = None
username = None

with open(log_file, 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

for line in lines:
    if "[Register-25]" not in line:
        continue
        
    # 提取用户名
    if '"username":"' in line and username is None:
        match = re.search(r'"username":"(\d+)"', line)
        if match:
            username = match.group(1)
            
    # 提取RSA密文完整
    if "RSA密文完整:" in line:
        match = re.search(r'RSA密文完整: (.+)$', line)
        if match:
            rsa_encrypted = match.group(1).strip()
            
    # 提取请求体JSON（包含DES密文）
    if "请求体JSON:" in line:
        match = re.search(r'请求体JSON: \{"value":"(.+?)"\}', line)
        if match:
            des_encrypted = match.group(1)
            break  # 第一个用户的数据已收集完

print("=" * 80)
print("使用Java生成的加密数据测试服务器")
print("=" * 80)
print(f"用户名: {username}")
print(f"RSA密文长度: {len(rsa_encrypted) if rsa_encrypted else 0}")
print(f"RSA密文前120字符: {rsa_encrypted[:120] if rsa_encrypted else 'N/A'}")
print(f"DES密文长度: {len(des_encrypted) if des_encrypted else 0}")
print(f"DES密文前80字符: {des_encrypted[:80] if des_encrypted else 'N/A'}")

if all([rsa_encrypted, des_encrypted]):
    website_url = "https://www.wwwtk666.com"
    
    # 构造请求头（完全按照Java代码）
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36',
        'Accept': 'application/json, text/plain, */*',
        'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
        'Cache-Control': 'no-cache',
        'Content-Type': 'application/json; charset=utf-8',
        'Pragma': 'no-cache',
        'Priority': 'u=1, i',
        'Referer': f'{website_url}/',
        'Sec-Ch-Ua': '"Chromium";v="142", "Google Chrome";v="142", "Not_A Brand";v="99"',
        'Sec-Ch-Ua-Mobile': '?0',
        'Sec-Ch-Ua-Platform': '"Linux"',
        'Sec-Fetch-Dest': 'empty',
        'Sec-Fetch-Mode': 'cors',
        'Sec-Fetch-Site': 'same-origin',
        'Origin': website_url,
        'device': 'web',
        'language': 'BN',
        'merchant': 'ck555bdtf3',
        'encryption': rsa_encrypted,  # Java生成的RSA密文
    }
    
    # 请求体
    data = json.dumps({"value": des_encrypted}, separators=(',', ':'))
    
    print("\n" + "=" * 80)
    print("发送请求到服务器")
    print("=" * 80)
    print(f"URL: {website_url}/wps/member/register")
    print(f"Method: PUT")
    print(f"请求体长度: {len(data)}")
    
    try:
        response = requests.put(
            f'{website_url}/wps/member/register',
            headers=headers,
            data=data,
            verify=False,
            timeout=15
        )
        
        print(f"\n响应状态码: {response.status_code}")
        print(f"响应内容:")
        
        try:
            res = response.json()
            print(json.dumps(res, indent=2, ensure_ascii=False))
            
            if res.get('success'):
                print(f"\n✅ 使用Java加密数据注册成功！")
                if res.get('value', {}).get('token'):
                    print(f"Token: {res['value']['token']}")
            else:
                error_code = res.get('errorCode', 'unknown')
                print(f"\n❌ 注册失败: {error_code}")
                
                if error_code == 'decryption.err':
                    print("\n【分析】decryption.err 错误说明服务器无法解密Java的RSA加密")
                    print("原因：Java使用PKCS1Padding（随机padding），与服务器期望的加密方式不匹配")
                elif error_code == 'exists.username.err':
                    print("\n【分析】用户名已存在说明加密解密都正确！")
                    print("这证明Java的加密逻辑是正确的")
        except:
            print(response.text)
            
    except Exception as e:
        print(f"❌ 请求失败: {e}")
else:
    print("\n❌ 无法从日志中提取完整的加密数据")
