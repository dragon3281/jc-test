import SockJS from 'sockjs-client/dist/sockjs.min.js'
import Stomp from 'stompjs'

class WebSocketClient {
  constructor() {
    this.stompClient = null
    this.connected = false
    this.subscriptions = new Map()
    this.reconnectTimer = null
    this.reconnectDelay = 3000
    this.maxReconnectDelay = 30000  // 最大重连延迟30秒
    this.reconnectAttempts = 0  // 重连尝试次数
  }

  /**
   * 连接WebSocket
   */
  connect(onConnected, onError) {
    if (this.connected) {
      console.log('[WebSocket] 已连接，跳过重复连接')
      if (onConnected) onConnected()
      return
    }

    const token = localStorage.getItem('token')
    const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`
    
    console.log('[WebSocket] 开始连接...', wsUrl)
    
    try {
      const socket = new SockJS(wsUrl)
      this.stompClient = Stomp.over(socket)
      
      // 启用STOMP原生心跳机制，与后端配置保持一致
      // 格式: [outgoing, incoming] 单位:毫秒
      this.stompClient.heartbeat.outgoing = 20000  // 发送心跳间隔20秒
      this.stompClient.heartbeat.incoming = 20000  // 接收心跳间隔20秒
      
      // 启用调试日志（可通过debug=null关闭）
      this.stompClient.debug = (msg) => {
        if (msg.indexOf('PING') === -1 && msg.indexOf('PONG') === -1) {
          console.log('[WebSocket Debug]', msg)
        }
      }
      
      const headers = token ? { Authorization: `Bearer ${token}` } : {}
      
      this.stompClient.connect(
        headers,
        (frame) => {
          console.log('[WebSocket] 连接成功', frame)
          this.connected = true
          this.reconnectAttempts = 0  // 重置重连计数器
          this.reconnectDelay = 3000  // 重置重连延迟
          
          console.log('[WebSocket] STOMP 心跳已启用: 发送/接收 = 20s/20s')
          
          // 重新订阅之前的主题
          this.resubscribeAll()
          
          if (onConnected) onConnected()
        },
        (error) => {
          console.error('[WebSocket] 连接失败', error)
          this.connected = false
          
          if (onError) onError(error)
          
          // 自动重连
          this.scheduleReconnect(onConnected, onError)
        }
      )
    } catch (error) {
      console.error('[WebSocket] 初始化失败', error)
      if (onError) onError(error)
    }
  }

  /**
   * 订阅主题
   */
  subscribe(topic, callback) {
    if (!this.stompClient || !this.connected) {
      console.warn('[WebSocket] 未连接，保存订阅配置待连接后自动订阅:', topic)
      this.subscriptions.set(topic, { callback, subscription: null })
      return null
    }

    console.log('[WebSocket] 订阅主题:', topic)
    
    const subscription = this.stompClient.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body)
        console.log('[WebSocket] 收到消息:', topic, data)
        callback(data)
      } catch (error) {
        console.error('[WebSocket] 消息解析失败:', error, message.body)
      }
    })

    this.subscriptions.set(topic, { callback, subscription })
    return subscription
  }

  /**
   * 取消订阅
   */
  unsubscribe(topic) {
    const sub = this.subscriptions.get(topic)
    if (sub && sub.subscription) {
      console.log('[WebSocket] 取消订阅:', topic)
      sub.subscription.unsubscribe()
    }
    this.subscriptions.delete(topic)
  }

  /**
   * 重新订阅所有主题
   */
  resubscribeAll() {
    console.log('[WebSocket] 重新订阅所有主题, 数量:', this.subscriptions.size)
    this.subscriptions.forEach((sub, topic) => {
      if (this.stompClient && this.connected) {
        const subscription = this.stompClient.subscribe(topic, (message) => {
          try {
            const data = JSON.parse(message.body)
            console.log('[WebSocket] 收到消息:', topic, data)
            sub.callback(data)
          } catch (error) {
            console.error('[WebSocket] 消息解析失败:', error)
          }
        })
        sub.subscription = subscription
        console.log('[WebSocket] 已重新订阅:', topic)
      }
    })
  }

  /**
   * 发送消息
   */
  send(destination, body) {
    if (!this.stompClient || !this.connected) {
      console.warn('[WebSocket] 未连接，无法发送消息')
      return false
    }
    
    this.stompClient.send(destination, {}, JSON.stringify(body))
    return true
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.stompClient && this.connected) {
      console.log('[WebSocket] 断开连接')
      this.stompClient.disconnect()
      this.connected = false
      this.subscriptions.clear()
    }
  }

  /**
   * 计划重连（指数退避策略）
   */
  scheduleReconnect(onConnected, onError) {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }

    // 指数退避：每次失败后延迟时间加個
    this.reconnectAttempts++
    const delay = Math.min(this.reconnectDelay * this.reconnectAttempts, this.maxReconnectDelay)
    
    console.log(`[WebSocket] 第 ${this.reconnectAttempts} 次重连尝试，${delay / 1000}秒后执行...`)
    
    this.reconnectTimer = setTimeout(() => {
      console.log('[WebSocket] 尝试重连...')
      this.connect(onConnected, onError)
    }, delay)
  }

  /**
   * 检查连接状态
   */
  isConnected() {
    return this.connected
  }
}

// 单例模式
const wsClient = new WebSocketClient()

export default wsClient
