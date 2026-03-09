import request from './request'
import imageCompression from 'browser-image-compression'

// 压缩图片 - 更激进的压缩配置
async function compressImage(file, options = {}) {
  // 如果图片已经小于 0.5MB，不压缩
  if (file.size < 500 * 1024) {
    console.log(`图片已小于 500KB，跳过压缩：${(file.size / 1024 / 1024).toFixed(2)}MB`)
    return file
  }

  const defaultOptions = {
    maxSizeMB: 0.8, // 最大 0.8MB，留有余量
    maxWidthOrHeight: 1280, // 减小最大尺寸
    useWebWorker: true,
    initialQuality: 0.7, // 降低初始质量
    maxWidth: 1280,
    maxHeight: 1280
  }

  const finalOptions = { ...defaultOptions, ...options }

  try {
    console.log(`开始压缩图片：${(file.size / 1024 / 1024).toFixed(2)}MB`)
    const compressedFile = await imageCompression(file, finalOptions)

    // 确保保留原始文件名和类型
    const newFile = new File([compressedFile], file.name, {
      type: 'image/jpeg'
    })

    console.log(`✅ 压缩完成：${(newFile.size / 1024 / 1024).toFixed(2)}MB`)
    return newFile
  } catch (error) {
    console.error('❌ 图片压缩失败:', error)
    // 如果压缩失败，尝试只转换格式
    try {
      const compressedFile = await imageCompression(file, {
        maxSizeMB: 1,
        maxWidthOrHeight: 1920
      })
      const newFile = new File([compressedFile], file.name, {
        type: 'image/jpeg'
      })
      console.log(`✅ 备用方案压缩完成：${(newFile.size / 1024 / 1024).toFixed(2)}MB`)
      return newFile
    } catch (error2) {
      console.error('❌ 备用方案也失败，返回原文件')
      return file // 压缩失败返回原文件
    }
  }
}

export function uploadImage(file) {
  return new Promise(async (resolve, reject) => {
    try {
      // 压缩图片
      const compressedFile = await compressImage(file)
      console.log(`最终上传文件大小：${(compressedFile.size / 1024 / 1024).toFixed(2)}MB`)

      const formData = new FormData()
      formData.append('file', compressedFile)

      const res = await request({
        url: '/files/upload-image',
        method: 'post',
        data: formData,
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      resolve(res)
    } catch (error) {
      console.error('❌ 上传失败:', error)
      reject(error)
    }
  })
}

export function deleteImage(imageUrl) {
  return request({
    url: '/files/delete-image',
    method: 'delete',
    params: { imageUrl }
  })
}
