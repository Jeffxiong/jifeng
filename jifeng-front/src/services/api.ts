// 使用相对路径，通过Vite代理转发
const API_BASE_URL = '';

// 获取Token
const getToken = (): string | null => {
  return localStorage.getItem('token');
};

// 设置Token
const setToken = (token: string): void => {
  localStorage.setItem('token', token);
};

// 移除Token
const removeToken = (): void => {
  localStorage.removeItem('token');
};

// 统一响应类型
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

// 通用请求方法
const request = async <T>(
  url: string,
  options: RequestInit = {}
): Promise<T> => {
  const token = getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json;charset=UTF-8',
    'Accept': 'application/json;charset=UTF-8',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      ...options,
      headers,
    });

    const data: ApiResponse<T> = await response.json();
    console.log('API Response:', url, data);

    // Token过期，清除并跳转登录
    if (data.code === 401) {
      removeToken();
      throw new Error(data.message || '登录已过期，请重新登录');
    }

    if (!response.ok || data.code !== 200) {
      const errorMessage = data.message || data.error || `HTTP错误: ${response.status}`;
      console.error('API Error:', url, response.status, errorMessage);
      throw new Error(errorMessage);
    }

    return data.data;
  } catch (error) {
    console.error('API Error:', url, error);
    if (error instanceof Error) {
      throw error;
    }
    throw new Error('网络错误，请稍后重试');
  }
};

// 认证API
export const authApi = {
  // 登录
  login: async (username: string, password: string) => {
    const response = await request<{
      token: string;
      refreshToken: string;
      expiresIn: number;
      userInfo: {
        userId: string; // UUID
        username: string;
        nickname: string;
      };
    }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    
    setToken(response.token);
    return response;
  },

  // 登出
  logout: () => {
    removeToken();
  },
};

// 积分API
export const pointsApi = {
  // 获取积分余额
  getBalance: async (): Promise<number> => {
    return request<number>('/api/points/balance');
  },

  // 获取积分明细
  getRecords: async (
    type: 'all' | 'earned' | 'spent' = 'all',
    timeRange: '30days' | '3months' | '12months' | '2years' = '30days'
  ) => {
    return request<any[]>(
      `/api/points/records?type=${type}&timeRange=${timeRange}`
    );
  },

  // 发送短信验证码
  sendSmsCode: async (): Promise<string> => {
    return request<string>('/api/points/send-sms-code', {
      method: 'POST',
    });
  },

  // 兑换产品（productId 改为 string UUID）
  exchange: async (
    productId: string,
    quantity: number,
    verificationCode: string
  ) => {
    return request<void>('/api/points/exchange', {
      method: 'POST',
      body: JSON.stringify({
        productId,
        quantity,
        verificationCode,
      }),
    });
  },
};

// 产品API
export const productApi = {
  // 获取产品列表
  getProducts: async () => {
    return request<any[]>('/api/products');
  },

  // 获取产品详情（id 改为 string UUID）
  getProduct: async (id: string) => {
    return request<any>(`/api/products/${id}`);
  },
};
