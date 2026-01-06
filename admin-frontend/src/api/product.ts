import api from './auth'

export interface Product {
  id: string  // UUID
  name: string
  points: number
  description: string
  stock: number
  image: string
  monthlyLimit: number
  status: number
  usedThisMonth?: number
}

export const getAllProducts = async () => {
  const response = await api.get('/products/admin/all')
  return response as Product[]
}

export const updateProduct = async (id: string, data: Partial<Product>) => {
  const response = await api.put(`/products/admin/${id}`, data)
  return response as Product
}

export const updateProductStock = async (id: string, stock: number) => {
  const response = await api.put(`/products/admin/${id}/stock`, { stock })
  return response as Product
}

export const updateProductStatus = async (id: string, status: number) => {
  const response = await api.put(`/products/admin/${id}/status`, { status })
  return response as Product
}

export const createProduct = async (data: Omit<Product, 'id' | 'usedThisMonth'>) => {
  const response = await api.post('/products/admin', data)
  return response as Product
}
