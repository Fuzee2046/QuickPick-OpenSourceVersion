<template>
  <van-popup
    :show="show"
    position="bottom"
    :style="{ height: '100vh' }"
    @update:show="handleVisibilityChange"
    @closed="handleClosed"
  >
    <div class="camera-dialog">
      <div class="camera-dialog__header">
        <div class="camera-dialog__title">{{ title }}</div>
        <button class="camera-dialog__close" @click="closeDialog">关闭</button>
      </div>

      <div class="camera-dialog__body">
        <div v-if="cameraError" class="camera-dialog__notice">
          <div class="camera-dialog__notice-title">相机暂时无法打开</div>
          <div class="camera-dialog__notice-text">{{ cameraError }}</div>
        </div>

        <div v-else-if="capturedPreviewUrl" class="camera-dialog__preview">
          <img :src="capturedPreviewUrl" alt="拍照预览" class="camera-dialog__preview-image" />
          <div class="camera-dialog__hint">请确认电子秤读数和餐品都拍清楚了</div>
        </div>

        <div v-else class="camera-dialog__preview">
          <video
            ref="videoRef"
            class="camera-dialog__video"
            autoplay
            muted
            playsinline
          ></video>
          <div class="camera-dialog__hint">
            请将电子秤读数放在画面中央，保持光线充足
          </div>
        </div>
      </div>

      <div class="camera-dialog__footer">
        <template v-if="capturedFile">
          <button class="camera-dialog__btn camera-dialog__btn--secondary" @click="retakePhoto">
            重拍
          </button>
          <button class="camera-dialog__btn camera-dialog__btn--primary" @click="confirmCapturedPhoto">
            使用这张照片
          </button>
        </template>
        <template v-else>
          <button class="camera-dialog__btn camera-dialog__btn--secondary" @click="triggerAlbumPick">
            从相册选择
          </button>
          <button
            class="camera-dialog__btn camera-dialog__btn--primary"
            :disabled="cameraStarting || !cameraReady"
            @click="capturePhoto"
          >
            {{ cameraStarting ? '正在打开相机...' : '立即拍照' }}
          </button>
        </template>
      </div>

      <input
        ref="albumInputRef"
        class="camera-dialog__album-input"
        type="file"
        accept="image/*"
        @change="handleAlbumSelect"
      />
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { showToast } from 'vant';

const props = withDefaults(defineProps<{
  show: boolean;
  title?: string;
}>(), {
  title: '拍照留证'
});

const emit = defineEmits<{
  'update:show': [value: boolean];
  captured: [file: File];
}>();

const videoRef = ref<HTMLVideoElement | null>(null);
const albumInputRef = ref<HTMLInputElement | null>(null);
const mediaStream = ref<MediaStream | null>(null);
const cameraStarting = ref(false);
const cameraReady = ref(false);
const cameraError = ref('');
const capturedFile = ref<File | null>(null);
const capturedPreviewUrl = ref('');

const revokePreviewUrl = () => {
  if (capturedPreviewUrl.value) {
    URL.revokeObjectURL(capturedPreviewUrl.value);
    capturedPreviewUrl.value = '';
  }
};

const resetCapturedState = () => {
  capturedFile.value = null;
  revokePreviewUrl();
};

const stopCameraStream = () => {
  if (mediaStream.value) {
    mediaStream.value.getTracks().forEach(track => track.stop());
    mediaStream.value = null;
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null;
  }
  cameraReady.value = false;
  cameraStarting.value = false;
};

const startCamera = async () => {
  if (!props.show || capturedFile.value) return;
  if (!navigator.mediaDevices?.getUserMedia) {
    cameraError.value = '当前浏览器不支持网页直接拍照，请改用相册上传';
    return;
  }

  stopCameraStream();
  cameraStarting.value = true;
  cameraError.value = '';

  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: {
        facingMode: { ideal: 'environment' },
        width: { ideal: 1280 },
        height: { ideal: 720 }
      },
      audio: false
    });

    mediaStream.value = stream;
    await nextTick();

    if (videoRef.value) {
      videoRef.value.srcObject = stream;
      await videoRef.value.play();
    }

    cameraReady.value = true;
  } catch (error: any) {
    cameraError.value = error?.name === 'NotAllowedError'
      ? '未获得相机权限，请允许浏览器使用摄像头，或改用相册上传'
      : '相机启动失败，请改用相册上传';
    stopCameraStream();
  } finally {
    cameraStarting.value = false;
  }
};

const closeDialog = () => {
  emit('update:show', false);
};

const handleVisibilityChange = (value: boolean) => {
  emit('update:show', value);
};

const handleClosed = () => {
  stopCameraStream();
  resetCapturedState();
  cameraError.value = '';
  if (albumInputRef.value) {
    albumInputRef.value.value = '';
  }
};

const buildCapturedFile = async () => {
  const video = videoRef.value;
  if (!video || !video.videoWidth || !video.videoHeight) {
    throw new Error('相机画面还未准备好，请稍后重试');
  }

  const canvas = document.createElement('canvas');
  canvas.width = video.videoWidth;
  canvas.height = video.videoHeight;
  const context = canvas.getContext('2d');

  if (!context) {
    throw new Error('当前设备不支持拍照，请改用相册上传');
  }

  context.drawImage(video, 0, 0, canvas.width, canvas.height);

  const blob = await new Promise<Blob | null>((resolve) => {
    canvas.toBlob(resolve, 'image/jpeg', 0.92);
  });

  if (!blob) {
    throw new Error('照片生成失败，请重试');
  }

  const fileName = `price-evidence-${Date.now()}.jpg`;
  return new File([blob], fileName, { type: 'image/jpeg' });
};

const capturePhoto = async () => {
  if (cameraStarting.value) return;
  if (!cameraReady.value) {
    showToast('相机正在准备，请稍后重试');
    return;
  }

  try {
    const file = await buildCapturedFile();
    capturedFile.value = file;
    revokePreviewUrl();
    capturedPreviewUrl.value = URL.createObjectURL(file);
    stopCameraStream();
  } catch (error: any) {
    showToast(error?.message || '拍照失败，请重试');
  }
};

const retakePhoto = async () => {
  resetCapturedState();
  await startCamera();
};

const confirmCapturedPhoto = () => {
  if (!capturedFile.value) return;
  emit('captured', capturedFile.value);
  closeDialog();
};

const triggerAlbumPick = () => {
  albumInputRef.value?.click();
};

const handleAlbumSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  if (!file.type.startsWith('image/')) {
    showToast('只能选择图片文件');
    target.value = '';
    return;
  }

  emit('captured', file);
  target.value = '';
  closeDialog();
};

watch(() => props.show, async (visible) => {
  if (visible) {
    resetCapturedState();
    await startCamera();
    return;
  }
  stopCameraStream();
}, { immediate: true });

onBeforeUnmount(() => {
  stopCameraStream();
  revokePreviewUrl();
});
</script>

<style scoped>
.camera-dialog {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  height: 100%;
  background: #0f172a;
  color: #fff;
}

.camera-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
}

.camera-dialog__title {
  font-size: 18px;
  font-weight: 700;
}

.camera-dialog__close {
  border: none;
  border-radius: 999px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  font-size: 13px;
}

.camera-dialog__body {
  min-height: 0;
  padding: 0 16px 16px;
}

.camera-dialog__preview {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 12px;
}

.camera-dialog__video,
.camera-dialog__preview-image {
  width: 100%;
  height: calc(100vh - 190px);
  border-radius: 20px;
  object-fit: cover;
  background: #020617;
}

.camera-dialog__hint {
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.78);
}

.camera-dialog__notice {
  margin-top: 12px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.camera-dialog__notice-title {
  font-size: 15px;
  font-weight: 700;
}

.camera-dialog__notice-text {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.78);
}

.camera-dialog__footer {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 14px 16px calc(16px + env(safe-area-inset-bottom));
}

.camera-dialog__btn {
  height: 46px;
  border: none;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
}

.camera-dialog__btn--primary {
  background: #f97316;
  color: #fff;
}

.camera-dialog__btn--primary:disabled {
  opacity: 0.6;
}

.camera-dialog__btn--secondary {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.camera-dialog__album-input {
  display: none;
}
</style>
