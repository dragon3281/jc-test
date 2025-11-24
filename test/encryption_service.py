#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
加密服务 - 为Java提供与JS完全一致的加密能力
Usage: python3 encryption_service.py <operation> <args...>
Operations:
  - rsa_encrypt <public_key_hex> <plaintext>
  - des_encrypt <plaintext> <key>
"""

import sys
import execjs
from pathlib import Path

script_dir = Path(__file__).parent

def rsa_encrypt(public_key_hex, plaintext):
    """RSA加密 - 使用JS脚本"""
    try:
        with open(script_dir / '加密逻辑.js', 'r', encoding='utf-8') as f:
            js_code = f.read()
        ctx = execjs.compile(js_code)
        result = ctx.call('rsaEncryptV2', public_key_hex, plaintext)
        return result
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return None

def des_encrypt(plaintext, key):
    """DES加密 - 使用JS脚本"""
    try:
        with open(script_dir / 'des加解密.js', 'r', encoding='utf-8') as f:
            js_code = f.read()
        ctx = execjs.compile(js_code)
        result = ctx.call('DES_Encrypt', plaintext, key)
        return result
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return None

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: encryption_service.py <operation> <args...>", file=sys.stderr)
        sys.exit(1)
    
    operation = sys.argv[1]
    
    if operation == "rsa_encrypt" and len(sys.argv) == 4:
        result = rsa_encrypt(sys.argv[2], sys.argv[3])
        if result:
            print(result)
        else:
            sys.exit(1)
            
    elif operation == "des_encrypt" and len(sys.argv) == 4:
        result = des_encrypt(sys.argv[2], sys.argv[3])
        if result:
            print(result)
        else:
            sys.exit(1)
            
    else:
        print(f"Unknown operation or invalid arguments: {operation}", file=sys.stderr)
        sys.exit(1)
