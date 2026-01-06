import api from './auth'

export interface ExchangeRecord {
  id: string  // UUID
  userId: string  // UUID
  username?: string
  nickname?: string
  phone?: string  // 用户手机号
  productId: string  // UUID
  productName?: string
  quantity: number
  points: number
  status: string
  couponCode?: string
  createdAt: string
  updatedAt?: string
}

export const getExchangeRecords = async (params?: {
  userId?: string  // UUID
  productId?: string  // UUID
  status?: string
}) => {
  const response = await api.get('/points/admin/exchanges', { params })
  return response as ExchangeRecord[]
}
