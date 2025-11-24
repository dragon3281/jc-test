#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试Java生成的加密数据是否能成功注册
"""

import requests
import json
import urllib3

# 禁用SSL警告
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# 从Java日志中复制的数据
rsa_encrypted = "0b85cdacae5ee354e394debdd3fbb0606d67dbddef490be223472abc653a3c01932bf0440a97d7458075557e8e0fc1e8f5152625d8783bb2415fcc5351919dd4a47af6ddd606fdee3a986c5391ba41cf3b89f43e1cd830353b6792af4475025d7d0ef09d942c09c3087f5959fb10d3f1a24da3318814019d19feb55ff5b64e18"

des_encrypted = "nO+xhGxb1j6YUa4tZP9KEH1r1u4NPoO/lMx35F7oxpZ8l5FZcKkRv5Rv3pdVCqucB6IQGm605hokdyPViBPWqLD1k4hMr2qn/11wpmRLa59iO0mM3B7VQ+IDf8edbRwMzlpVLBwtlaihQ3ZIkX+9b5ORPGvqMZWFwVSYMqscyTAfZKP2ib4ZZ0lW4Iw0txDv+TntedZZtMPXaILpw8AZyhPyLmpTKzKYZCZkXl403MfLmBKdeL7LeSPHFT7aDw5FJHcj1YgT1qgIaPKfw9C/AnOrBX7k/Fcpkm1q6GcDXdAYIaubXQ3tQC8ftkvFzqVJoGXygfzjYcdvGWLAuWSq50vIlaNe/3wftH8LtKUqKx+PeRmxodJhD5rAQlt5cTyfVs2516vu+Cuz+/GZjz16E3pqZ4fpbK8w8zov6dzYyvssEqxHOtGhA8Y166OHf6ZRlu4bHWKM8RnAxjyDow9Epk+1RLqx203T8MYsx7r6lJUQA58RaBf3zMWO1SWVVbQA3sq0/dIqjyExSxQOdvLitQGLvL0dPqXz2jgWGpTkfnXjPgFAOeQn4iPCAIpjDvsi6E19ilPLvgs="

website_url = "https://www.wwwtk666.com"

# 使用与Python脚本相同的请求头
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

register_json_data = {
    'value': des_encrypted
}

data = json.dumps(register_json_data, separators=(',', ':'))

print("=== 使用Java生成的加密数据进行测试 ===")
print(f"RSA密文长度: {len(rsa_encrypted)}")
print(f"DES密文长度: {len(des_encrypted)}")
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
        print("\n✅ 注册成功！Java生成的数据是正确的！")
        if res.get('value', {}).get('token'):
            print(f"Token: {res['value']['token']}")
    else:
        print(f"\n❌ 注册失败: {res.get('errorCode')}")
        print(f"错误信息: {res.get('message')}")
        
except Exception as e:
    print(f"❌ 请求失败: {e}")
