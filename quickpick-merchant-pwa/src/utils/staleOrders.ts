import request from '@/utils/request';

export interface StaleOrderPage {
  records: any[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export const fetchStaleOrders = async (page = 1, pageSize = 20): Promise<StaleOrderPage> => {
  const result: any = await request.get('/api/merchant/orders/stale', {
    params: { page, pageSize },
  });
  return {
    records: Array.isArray(result?.records) ? result.records : [],
    total: Number(result?.total || 0),
    page: Number(result?.page || page),
    pageSize: Number(result?.pageSize || pageSize),
    totalPages: Number(result?.totalPages || 0),
  };
};

export const hasStaleOrders = async () => {
  const result = await fetchStaleOrders(1, 1);
  return result.total > 0;
};
