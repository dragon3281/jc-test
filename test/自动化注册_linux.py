#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
自动化注册脚本 - Linux 适配版本
支持 DES + RSA 双重加密的网站自动注册
"""

import json
import random
import time
import requests
import subprocess
import sys
from pathlib import Path

# Linux 默认使用 utf-8 编码，无需特殊配置
import execjs

# 代理配置（可选，Linux 上可能不需要代理）
USE_PROXY = False
proxies = {
    'http': 'http://127.0.0.1:10808',
    'https': 'http://127.0.0.1:10808'
} if USE_PROXY else None

def generate_11_digit_number():
    """生成首位非0的11位数字"""
    first_digit = random.randint(1, 9)
    other_digits = [random.randint(0, 9) for _ in range(10)]
    digits = [str(first_digit)] + [str(d) for d in other_digits]
    return ''.join(digits)

def rnd_string():
    """生成16位随机字符串"""
    chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz"
    result = ""
    for _ in range(16):
        index = int(61 * random.random())
        result += chars[index]
    return result

def register_account(website_url="https://www.wwwtk666.com", output_file="./token.txt"):
    """
    执行单次注册
    
    Args:
        website_url: 目标网站 URL
        output_file: Token 输出文件路径
    
    Returns:
        dict: 包含注册结果的字典 {'success': bool, 'token': str, 'username': str, 'response': dict}
    """
    
    # 获取脚本所在目录
    script_dir = Path(__file__).parent
    
    cookies = {
        'SHELL_deviceId': '772e0b20-91c1-41c5-a522-6f1a9585adbc',
    }

    headers = {
        'Host': website_url.replace('https://', '').replace('http://', ''),
        'Pragma': 'no-cache',
        'Cache-Control': 'no-cache',
        'Sec-Ch-Ua-Platform': '"Linux"',
        'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36',
        'Sec-Ch-Ua': '"Chromium";v="142", "Google Chrome";v="142", "Not_A Brand";v="99"',
        'Sec-Ch-Ua-Mobile': '?0',
        'Accept': '*/*',
        'Sec-Fetch-Site': 'same-origin',
        'Sec-Fetch-Mode': 'cors',
        'Sec-Fetch-Dest': 'empty',
        'Referer': f'{website_url}/',
        'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
        'Priority': 'u=1, i',
    }
    
    # 步骤1: 获取 RSA 公钥
    url = f"{website_url}/wps/session/key/rsa?t={int(time.time() * 1000)}"
    print(f"[1/5] 获取RSA公钥: {url}")
    
    try:
        response = requests.get(
            url=url,
            cookies=cookies,
            headers=headers,
            proxies=proxies,
            verify=False,
            timeout=15
        )
        key = response.text
        print(f"[RSA公钥] {key}")
    except Exception as e:
        print(f"❌ 获取RSA公钥失败: {e}")
        return {'success': False, 'error': str(e)}
    
    # 步骤2: 生成随机字符串和密钥
    rnd = rnd_string()
    reversed_string = rnd[::-1]
    print(f"\n[2/5] 生成加密密钥:")
    print(f"  原始随机字符串 (rnd): {rnd}")
    print(f"  反转字符串 (DES密钥): {reversed_string}")
    
    # 步骤3: RSA加密原始rnd
    print(f"\n[3/5] RSA加密 (加密原始rnd)")
    try:
        with open(script_dir / '加密逻辑.js', 'r', encoding='utf-8') as f:
            key_code = f.read()
        ctx2 = execjs.compile(key_code)
        encryptions = ctx2.call('rsaEncryptV2', key, rnd)
        print(f"  RSA密文长度: {len(encryptions)}")
        print(f"  RSA密文前120字符: {encryptions[:120]}")
    except Exception as e:
        print(f"❌ RSA加密失败: {e}")
        return {'success': False, 'error': str(e)}
    
    # 步骤4: 构造注册数据并DES加密
    username = generate_11_digit_number()
    dict_value = {
        "username": username,
        "password": "133adb",
        "confirmPassword": "133adb",
        "payeeName": "",
        "email": "",
        "qqNum": "",
        "mobileNum": "",
        "captcha": "",
        "verificationCode": "",
        "affiliateCode": "www",
        "paymentPassword": "",
        "line": "",
        "whatsapp": "",
        "facebook": "",
        "wechat": "",
        "idNumber": "",
        "nickname": "",
        "domain": "www-tk999",
        "login": True,
        "registerUrl": f"{website_url}/",
        "registerMethod": "WEB",
        "loginDeviceId": "e6ce5ac9-4b17-4e33-acbd-7350b443f572"
    }
    
    print(f"\n[4/5] DES加密注册数据 (使用反转的rnd作为密钥)")
    print(f"  用户名: {username}")
    print(f"  DES密钥: {reversed_string}")
    
    try:
        with open(script_dir / 'des加解密.js', 'r', encoding='utf-8') as f:
            js_code = f.read()
        ctx1 = execjs.compile(js_code)
        # 移除JSON中的空格
        json_str = json.dumps(dict_value).replace(" ", "")
        value = ctx1.call('DES_Encrypt', json_str, reversed_string)
        print(f"  DES密文长度: {len(value)}")
        print(f"  DES密文前80字符: {value[:80]}")
    except Exception as e:
        print(f"❌ DES加密失败: {e}")
        return {'success': False, 'error': str(e)}
    
    # 步骤5: 发送注册请求
    register_headers = {
        'accept': 'application/json, text/plain, */*',
        'accept-language': 'zh-CN,zh;q=0.9,en;q=0.8',
        'cache-control': 'no-cache',
        'content-type': 'application/json',
        'device': 'web',
        'encryption': encryptions,  # RSA加密的rnd
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
        'value': value  # DES加密的数据
    }
    data = json.dumps(register_json_data, separators=(',', ':'))
    
    print(f"\n[5/5] 发送注册请求到 {website_url}/wps/member/register")
    print(f"  请求头 encryption 长度: {len(encryptions)}")
    print(f"  请求体 value 长度: {len(value)}")
    
    try:
        register_response = requests.put(
            f'{website_url}/wps/member/register',
            headers=register_headers,
            data=data,
            proxies=proxies,
            verify=False,
            timeout=15
        )
        res = register_response.json()
        
        print(f"\n{'='*60}")
        print(f"注册响应:")
        print(f"{'='*60}")
        print(json.dumps(res, indent=2, ensure_ascii=False))
        
        # 提取token
        token = None
        actual_username = username
        if res.get('success') and res.get('value'):
            value_data = res['value']
            if isinstance(value_data, dict):
                token = value_data.get('token')
                actual_username = value_data.get('userName', username)
                
                print(f"\n✅ 注册成功!")
                print(f"  用户名: {actual_username}")
                print(f"  Token: {token}")
                
                # 保存token到文件
                if token:
                    try:
                        with open(output_file, 'a', encoding='utf-8') as f:
                            f.write(f"{token}\n")
                        print(f"  Token已保存到: {output_file}")
                    except Exception as e:
                        print(f"⚠️  保存Token失败: {e}")
                
                return {
                    'success': True,
                    'token': token,
                    'username': actual_username,
                    'response': res
                }
        
        print(f"\n❌ 注册失败")
        return {
            'success': False,
            'username': username,
            'response': res
        }
        
    except Exception as e:
        print(f"❌ 注册请求失败: {e}")
        return {'success': False, 'error': str(e)}

def main():
    """主函数"""
    print("="*60)
    print("自动化注册脚本 - Linux 版本")
    print("="*60)
    
    # 禁用SSL警告
    import urllib3
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    
    # 执行注册
    result = register_account()
    
    if result.get('success'):
        print("\n✅ 脚本执行成功!")
        print(f"返回值示例:")
        print(f"  RSA密文(encryption): {result.get('response', {}).get('encryption', 'N/A')[:120]}...")
        print(f"  DES密钥(reversedRnd): (见上方日志)")
        print(f"  原始密钥(rnd): (见上方日志)")
        print(f"  服务器响应: {result.get('response')}")
        sys.exit(0)
    else:
        print("\n❌ 脚本执行失败!")
        sys.exit(1)

if __name__ == "__main__":
    # 支持循环注册（可选）
    if len(sys.argv) > 1:
        try:
            count = int(sys.argv[1])
            success_count = 0
            for i in range(count):
                print(f"\n\n{'#'*60}")
                print(f"第 {i+1}/{count} 次注册")
                print(f"{'#'*60}")
                result = register_account()
                if result.get('success'):
                    success_count += 1
                time.sleep(1)  # 避免请求过快
            print(f"\n总计: {count} 次注册, 成功: {success_count}, 失败: {count - success_count}")
        except ValueError:
            print("❌ 参数错误，用法: python3 自动化注册_linux.py [次数]")
    else:
        main()
