import json
import hashlib
import random
import time
import re
from curl_cffi import requests
import subprocess
from functools import partial
subprocess.Popen = partial(subprocess.Popen, encoding="utf-8")
# 修改编码方式,window默认编码是gbk,Mac和Linux 默认是uft-8
import execjs


proxies = {
        'http': 'http://127.0.0.1:10808',
        'https': 'http://127.0.0.1:10808'
    }
def generate_11_digit_number():
    # 第一位不能为0，所以从1-9中选择
    first_digit = random.randint(1, 9)
    # 后面10位可以是0-9的任意数字
    other_digits = [random.randint(0, 9) for _ in range(10)]

    # 组合所有数字并转换为整数
    digits = [str(first_digit)] + [str(d) for d in other_digits]
    return int(''.join(digits))

print(generate_11_digit_number())

def rnd_string():
    # 定义字符集，与JavaScript版本保持一致
    chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz"
    result = ""
    # 生成16位随机字符串
    for _ in range(16):
        # 生成0到60之间的随机整数（字符集长度为61）
        index = int(61 * random.random())
        # 从字符集中取对应位置的字符并添加到结果
        result += chars[index]
    return result

# print(rnd_string())
for i in range(100):
    cookies = {
        'SHELL_deviceId': '772e0b20-91c1-41c5-a522-6f1a9585adbc',
    }

    headers = {
        'Host': 'www.wwwtk666.com',
        'Pragma': 'no-cache',
        'Cache-Control': 'no-cache',
        'Sec-Ch-Ua-Platform': '"Windows"',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36',
        'Sec-Ch-Ua': '"Chromium";v="142", "Google Chrome";v="142", "Not_A Brand";v="99"',
        'Sec-Ch-Ua-Mobile': '?0',
        'Accept': '*/*',
        'Sec-Fetch-Site': 'same-origin',
        'Sec-Fetch-Mode': 'cors',
        'Sec-Fetch-Dest': 'empty',
        'Referer': 'https://www.wwwtk666.com/',
        # 'Accept-Encoding': 'gzip, deflate, br',
        'Accept-Language': 'zh,zh-CN;q=0.9',
        'Priority': 'u=1, i',
        # 'Cookie': 'SHELL_deviceId=e6ce5ac9-4b17-4e33-acbd-7350b443f572',
    }
    url = "https://www.wwwtk666.com/wps/session/key/rsa?t={}".format(int(time.time() * 1000))
    # print(time.time() * 1000)

    response = requests.get(
        url=url,
        cookies=cookies,
        headers=headers,
        proxies=proxies,
        verify=False,
    )
    key = response.text
    print(key)
    rnd = rnd_string()
    reversed_string = rnd[::-1]
    # rnd = "mLATB2Zyaiqos3gS"
    # reversed_string = "Sg3soqiayZ2BTALm"
    print(rnd)
    print(reversed_string)
    with open('./加密逻辑.js', 'r', encoding='utf-8') as f:
        key_code = f.read()
    ctx2 = execjs.compile(key_code)
    encryptions = ctx2.call('rsaEncryptV2', key, rnd)
    # print("秘钥",encryptions)

    dict_value = {"username":generate_11_digit_number(),"password":"133adb","confirmPassword":"133adb","payeeName":"","email":"","qqNum":"","mobileNum":"","captcha":"","verificationCode":"","affiliateCode":"www","paymentPassword":"","line":"","whatsapp":"","facebook":"","wechat":"","idNumber":"","nickname":"","domain":"www-tk999","login":True,"registerUrl":"https://www.wwwtk666.com/","registerMethod":"WEB","loginDeviceId":"e6ce5ac9-4b17-4e33-acbd-7350b443f572"}
    with open('./des加解密.js', 'r', encoding='utf-8') as f:
        js_code = f.read()
    ctx1 = execjs.compile(js_code)
    # print(json.dumps(dict_value).replace(" ", ""))
    value = ctx1.call('DES_Encrypt', json.dumps(dict_value).replace(" ", ""), reversed_string)
    # print("密文",value)

    register_headers = {
        'accept': 'application/json, text/plain, */*',
        'accept-language': 'zh,zh-CN;q=0.9',
        'cache-control': 'no-cache',
        'content-type': 'application/json',
        'device': 'web',
        'encryption': encryptions,
        'language': 'BN',
        'merchant': 'ck555bdtf3',
        'origin': 'https://www.wwwtk666.com',
        'pragma': 'no-cache',
        'priority': 'u=1, i',
        'referer': 'https://www.wwwtk666.com/',
        'sec-ch-ua': '"Chromium";v="142", "Google Chrome";v="142", "Not_A Brand";v="99"',
        'sec-ch-ua-mobile': '?0',
        'sec-ch-ua-platform': '"Windows"',
        'sec-fetch-dest': 'empty',
        'sec-fetch-mode': 'cors',
        'sec-fetch-site': 'same-origin',
        'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36',
        # 'cookie': 'SHELL_deviceId=772e0b20-91c1-41c5-a522-6f1a9585adbc',
    }

    register_json_data = {
        'value': value
    }
    data = json.dumps(register_json_data, separators=(',', ':'))
    register_response = requests.put('https://www.wwwtk666.com/wps/member/register', headers=register_headers, data=data,proxies=proxies,verify=False)
    res = (register_response.json())
    print(res)
    try:
        with open('./token.txt', 'a', encoding='utf-8') as f:
            f.write(res['value']['token'] + '\n')
    except:
        # time.sleep(1)
        continue
