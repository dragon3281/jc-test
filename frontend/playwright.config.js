import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: 0,
  workers: 1,
  reporter: [
    ['html'],
    ['list'],
    ['json', { outputFile: 'test-results/results.json' }]
  ],
  
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on',  // 始终记录trace
    screenshot: 'on',  // 始终截图
    video: 'on',  // 始终录屏
    headless: true,  // 无头模式，适用于服务器环境
  },

  projects: [
    {
      name: 'chromium',
      use: { 
        ...devices['Desktop Chrome'],
        headless: true,  // 确保无头模式
      },
    },
  ],

  // 注释掉webServer配置，因为服务已经在运行
  // webServer: {
  //   command: 'npm run dev',
  //   url: 'http://103.246.244.229:3000',
  //   reuseExistingServer: true,
  //   timeout: 120000,
  // },
});
