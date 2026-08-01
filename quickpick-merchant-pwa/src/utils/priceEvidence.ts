import request from '@/utils/request';

export const uploadPriceEvidenceImage = async (file: File) => {
  const formData = new FormData();
  formData.append('file', file);

  try {
    return await request.post('/api/merchant/upload/price-evidence-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  } catch (error) {
    // 兼容本地后端未重启到最新接口时的测试场景，先回退到通用图片上传接口
    return await request.post('/api/merchant/upload/dish-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  }
};

export const buildUploadedPreviewFile = (url: string) => ({
  url,
  status: 'done',
  message: '上传成功'
});
