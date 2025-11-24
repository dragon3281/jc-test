<template>
  <div class="terminal-page">
    <div class="terminal-header">
      <div class="header-left">
        <el-icon class="header-icon"><Monitor /></el-icon>
        <span class="header-title">SSH终端 - {{ serverName }}</span>
        <el-tag v-if="connected" type="success">已连接</el-tag>
        <el-tag v-else type="danger">未连接</el-tag>
      </div>
      <el-button @click="closeTerminal" :icon="Close">关闭</el-button>
    </div>
    <div class="terminal-container">
      <div id="terminal" ref="terminalRef"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Monitor, Close } from '@element-plus/icons-vue'
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
import 'xterm/css/xterm.css'

const route = useRoute()
const serverId = ref(route.query.serverId)
const serverName = ref(route.query.serverName || '未知服务器')
const connected = ref(false)
const terminalRef = ref(null)

let terminal = null
let fitAddon = null
let websocket = null

onMounted(() => {
  initTerminal()
  connectWebSocket()
})

onBeforeUnmount(() => {
  if (websocket) {
    websocket.close()
  }
  if (terminal) {
    terminal.dispose()
  }
})

const initTerminal = () => {
  terminal = new Terminal({
    cursorBlink: true,
    cursorStyle: 'block',
    fontSize: 14,
    fontFamily: 'Consolas, Monaco, monospace',
    theme: {
      background: '#1e1e1e',
      foreground: '#d4d4d4',
      cursor: '#ffffff',
      selection: 'rgba(255, 255, 255, 0.3)',
      black: '#000000',
      red: '#cd3131',
      green: '#0dbc79',
      yellow: '#e5e510',
      blue: '#2472c8',
      magenta: '#bc3fbc',
      cyan: '#11a8cd',
      white: '#e5e5e5',
      brightBlack: '#666666',
      brightRed: '#f14c4c',
      brightGreen: '#23d18b',
      brightYellow: '#f5f543',
      brightBlue: '#3b8eea',
      brightMagenta: '#d670d6',
      brightCyan: '#29b8db',
      brightWhite: '#e5e5e5'
    },
    cols: 120,
    rows: 30
  })

  fitAddon = new FitAddon()
  terminal.loadAddon(fitAddon)

  terminal.open(terminalRef.value)
  fitAddon.fit()

  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    fitAddon.fit()
  })

  terminal.writeln('正在连接服务器...')
}

const connectWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  // 使用完整的URL地址，确保连接正确
  const host = window.location.hostname
  // 使用/terminal/ssh路径，避免与/ws STOMP端点冲突
  const wsUrl = `${protocol}//${host}:8080/terminal/ssh?serverId=${serverId.value}`
  
  console.log('WebSocket连接URL:', wsUrl)
  terminal.writeln(`正在连接到: ${wsUrl}`)

  try {
    websocket = new WebSocket(wsUrl)
  } catch (error) {
    terminal.writeln('\x1b[31m✗ WebSocket创建失败\x1b[0m')
    terminal.writeln(`错误: ${error.message}`)
    ElMessage.error('WebSocket创建失败')
    console.error('WebSocket creation error:', error)
    return
  }

  websocket.onopen = () => {
    connected.value = true
    terminal.clear()
    terminal.writeln('\x1b[32m✓ SSH连接已建立\x1b[0m')
    terminal.writeln('')
    
    // 监听终端输入
    terminal.onData((data) => {
      if (websocket.readyState === WebSocket.OPEN) {
        websocket.send(data)
      }
    })
  }

  websocket.onmessage = (event) => {
    terminal.write(event.data)
  }

  websocket.onerror = (error) => {
    connected.value = false
    terminal.writeln('\x1b[31m✗ WebSocket连接错误\x1b[0m')
    terminal.writeln(`请检查：`)
    terminal.writeln(`1. 后端服务是否启动 (8080端口)`)
    terminal.writeln(`2. 服务器ID: ${serverId.value} 是否存在`)
    terminal.writeln(`3. 服务器SSH凭证是否正确`)
    ElMessage.error('WebSocket连接错误，请检查后端服务')
    console.error('WebSocket error:', error)
  }

  websocket.onclose = (event) => {
    connected.value = false
    terminal.writeln(`\x1b[33m\r\n连接已关闭 (code: ${event.code}, reason: ${event.reason || '无'})\x1b[0m`)
    
    if (event.code === 1006) {
      terminal.writeln('\x1b[31m连接被异常关闭，可能原因：\x1b[0m')
      terminal.writeln('- 后端服务未启动或已崩溃')
      terminal.writeln('- 网络连接中断')
      terminal.writeln('- SSH服务器连接失败')
    }
    
    if (event.wasClean) {
      ElMessage.info('SSH连接已正常关闭')
    } else {
      ElMessage.warning('SSH连接已断开')
    }
  }
}

const closeTerminal = () => {
  window.close()
}
</script>

<style lang="scss" scoped>
.terminal-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #1e1e1e;
  display: flex;
  flex-direction: column;

  .terminal-header {
    background: #2d2d30;
    padding: 12px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #3e3e42;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .header-icon {
        color: #4ec9b0;
        font-size: 20px;
      }

      .header-title {
        color: #cccccc;
        font-size: 14px;
        font-weight: 500;
      }
    }
  }

  .terminal-container {
    flex: 1;
    padding: 10px;
    overflow: hidden;

    #terminal {
      width: 100%;
      height: 100%;
    }
  }
}
</style>
