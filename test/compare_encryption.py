#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
对比Python和Java的加密结果
"""

import json
import execjs
from pathlib import Path

script_dir = Path(__file__).parent

# 从Java日志复制的数据
java_json = '{"username":"49422513141","password":"133adb","confirmPassword":"133adb","payeeName":"","email":"","qqNum":"","mobileNum":"","captcha":"","verificationCode":"","affiliateCode":"www","paymentPassword":"","line":"","whatsapp":"","facebook":"","wechat":"","idNumber":"","nickname":"","domain":"www-tk999","login":true,"registerUrl":"https://www.wwwtk666.com/","registerMethod":"WEB","loginDeviceId":"e6ce5ac9-4b17-4e33-acbd-7350b443f572"}'

rnd = "REcrH70uBcRFSX2f"
reversed_rnd = "f2XSFRcBu07HrcER"

print("=== 对比加密 ===")
print(f"原始rnd: {rnd}")
print(f"反转rnd (DES密钥): {reversed_rnd}")
print(f"JSON长度: {len(java_json)}")
print()

# Python DES加密
with open(script_dir / 'des加解密.js', 'r', encoding='utf-8') as f:
    js_code = f.read()
ctx1 = execjs.compile(js_code)
python_des_result = ctx1.call('DES_Encrypt', java_json, reversed_rnd)

print("Python DES加密结果:")
print(f"长度: {len(python_des_result)}")
print(f"前80字符: {python_des_result[:80]}")
print()

# Java DES加密结果（从日志复制）
java_des_result = "nO+xhGxb1j6YUa4tZP9KEH1r1u4NPoO/lMx35F7oxpZ8l5FZcKkRv5Rv3pdVCqucB6IQGm605hokdyPViBPWqLD1k4hMr2qn/11wpmRLa59iO0mM3B7VQ+IDf8edbRwMzlpVLBwtlaihQ3ZIkX+9b5ORPGvqMZWFwVSYMqscyTAfZKP2ib4ZZ0lW4Iw0txDv+TntedZZtMPXaILpw8AZyhPyLmpTKzKYZCZkXl403MfLmBKdeL7LeSPHFT7aDw5FJHcj1YgT1qgIaPKfw9C/AnOrBX7k/Fcpkm1q6GcDXdAYIaubXQ3tQC8ftkvFzqVJoGXygfzjYcdvGWLAuWSq50vIlaNe/3wftH8LtKUqKx+PeRmxodJhD5rAQlt5cTyfVs2516vu+Cuz+/GZjz16E3pqZ4fpbK8w8zov6dzYyvssEqxHOtGhA8Y166OHf6ZRlu4bHWKM8RnAxjyDow9Epk+1RLqx203T8MYsx7r6lJUQA58RaBf3zMWO1SWVVbQA3sq0/dIqjyExSxQOdvLitQGLvL0dPqXz2jgWGpTkfnXjPgFAOeQn4iPCAIpjDvsi6E19ilPLvgs="

print("Java DES加密结果:")
print(f"长度: {len(java_des_result)}")
print(f"前80字符: {java_des_result[:80]}")
print()

if python_des_result == java_des_result:
    print("✅ DES加密结果完全一致！")
else:
    print("❌ DES加密结果不一致！")
    print(f"\nPython完整结果:\n{python_des_result}\n")
    print(f"\nJava完整结果:\n{java_des_result}\n")
