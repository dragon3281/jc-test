#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
使用Python成功的参数测试Java的加密结果
"""

import requests
import json
import urllib3
import execjs
from pathlib import Path

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

script_dir = Path(__file__).parent

# 从成功的Python日志中提取的参数
rnd = "mPTmmxbTc1uhAJai"
reversed_rnd = "iaJAhu1cTbxmmTPm"
rsa_public_key = "b6ce532032c208c99795cecb191343b0d8e5a27120f8a30dec8ce98900f2bd2722edc89a915116735cb60f82c310ee9ab6ac292e3c10659079645c919b2f35f3b62f82509e492e35419c22c093910e30c278f76cfaed2a870ad4409b7defd9f028ee9263d5530ba97b435665c36c93ac63f8020da6d4492319f146da3e4edbcb"
username = "22320588353"

print("=== 使用Python成功的参数 ===")
print(f"rnd: {rnd}")
print(f"reversed_rnd: {reversed_rnd}")
print(f"username: {username}")
print()

# 构造相同的JSON
java_json = f'{{"username":"{username}","password":"133adb","confirmPassword":"133adb","payeeName":"","email":"","qqNum":"","mobileNum":"","captcha":"","verificationCode":"","affiliateCode":"www","paymentPassword":"","line":"","whatsapp":"","facebook":"","wechat":"","idNumber":"","nickname":"","domain":"www-tk999","login":true,"registerUrl":"https://www.wwwtk666.com/","registerMethod":"WEB","loginDeviceId":"e6ce5ac9-4b17-4e33-acbd-7350b443f572"}}'

print(f"JSON长度: {len(java_json)}")
print(f"JSON: {java_json}")
print()

# RSA加密
with open(script_dir / '加密逻辑.js', 'r', encoding='utf-8') as f:
    key_code = f.read()
ctx2 = execjs.compile(key_code)
rsa_encrypted = ctx2.call('rsaEncryptV2', rsa_public_key, rnd)

print(f"RSA加密结果:")
print(f"  长度: {len(rsa_encrypted)}")
print(f"  内容: {rsa_encrypted}")
print()

# DES加密
with open(script_dir / 'des加解密.js', 'r', encoding='utf-8') as f:
    js_code = f.read()
ctx1 = execjs.compile(js_code)
des_encrypted = ctx1.call('DES_Encrypt', java_json, reversed_rnd)

print(f"DES加密结果:")
print(f"  长度: {len(des_encrypted)}")
print(f"  前80字符: {des_encrypted[:80]}")
print()

# 发送请求测试
website_url = "https://www.wwwtk666.com"
register_headers = {
    'accept': 'application/json, text/plain, */*',
    'accept-language': 'zh-CN,zh;q=0.9,en;q=0.8',
    'cache-control': 'no-cache',
    'content-type': 'application/json',
    'device': 'web',
    'encryption': rsa_encrypted,
    'language': 'BN',
    'merchant': 'ck555bdtf3',
    'origin': website_url,
    'pragma': 'no-cache',
    'priority': 'u=1, i',
    'referer': f'{website_url}/',
    'sec-ch-ua': '"Chromium";v="142", "Google Chrome";v="142", "Not_A Brand";v="99"',
    'sec-ch-ua-mobile': '?0',
    'sec-ch-ua-platform': '"Linux"',
    'sec-fetch-dest': 'empty',
    'sec-fetch-mode': 'cors',
    'sec-fetch-site': 'same-origin',
    'user-agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36',
}

register_json_data = {'value': des_encrypted}
data = json.dumps(register_json_data, separators=(',', ':'))

print(f"请求体: {data}")
print(f"请求体长度: {len(data)}")
print()

try:
    register_response = requests.put(
        f'{website_url}/wps/member/register',
        headers=register_headers,
        data=data,
        verify=False,
        timeout=15
    )
    res = register_response.json()
    
    print(f"响应状态码: {register_response.status_code}")
    print(f"响应内容:")
    print(json.dumps(res, indent=2, ensure_ascii=False))
    
    if res.get('success'):
        print(f"\n✅ 使用相同参数注册成功！")
        if res.get('value', {}).get('token'):
            print(f"Token: {res['value']['token']}")
    else:
        print(f"\n❌ 注册失败: {res.get('errorCode')}")
        
except Exception as e:
    print(f"❌ 请求失败: {e}")

print("\n=== 这些参数应该提供给Java使用 ===")
print(f"rnd={rnd}")
print(f"reversedRnd={reversed_rnd}")
print(f"RSA公钥={rsa_public_key}")
print(f"username={username}")
