import SockJS from 'sockjs-client/dist/sockjs.min.js'
import Stomp from 'stompjs'

class WebSocketClient {
  constructor() {
    this.stompClient = null
    this.connected = false
    this.subscriptions = new Map()
    this.reconnectTimer = null
    this.reconnectDelay = 3000
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
      
      // 禁用日志（可选）
      this.stompClient.debug = null
      
      const headers = token ? { Authorization: `Bearer ${token}` } : {}
      
      this.stompClient.connect(
        headers,
        (frame) => {
          console.log('[WebSocket] 连接成功', frame)
          this.connected = true
          
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
   * 计划重连
   */
  scheduleReconnect(onConnected, onError) {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }

    console.log(`[WebSocket] ${this.reconnectDelay / 1000}秒后尝试重连...`)
    
    this.reconnectTimer = setTimeout(() => {
      console.log('[WebSocket] 尝试重连...')
      this.connect(onConnected, onError)
    }, this.reconnectDelay)
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
