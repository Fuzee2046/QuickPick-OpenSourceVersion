const NOTIFICATION_SOUND_SRC = '/audio/new-order.MP3';
const NOTIFICATION_SOUND_ENABLED_KEY = 'notification_sound_enabled';

type BrowserAudioContext = AudioContext;

let audioContext: BrowserAudioContext | null = null;
let notificationSoundBuffer: AudioBuffer | null = null;
let loadingPromise: Promise<AudioBuffer | null> | null = null;

const getAudioContext = (): BrowserAudioContext | null => {
  if (typeof window === 'undefined') return null;

  const AudioContextCtor =
    window.AudioContext ||
    // @ts-expect-error webkitAudioContext exists on some mobile browsers
    window.webkitAudioContext;

  if (!AudioContextCtor) return null;

  if (!audioContext) {
    audioContext = new AudioContextCtor();
  }

  return audioContext;
};

const loadNotificationSound = async (ctx: BrowserAudioContext): Promise<AudioBuffer | null> => {
  if (notificationSoundBuffer) return notificationSoundBuffer;
  if (loadingPromise) return loadingPromise;

  loadingPromise = fetch(NOTIFICATION_SOUND_SRC)
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(`Failed to load notification sound: ${response.status}`);
      }
      const arrayBuffer = await response.arrayBuffer();
      return await ctx.decodeAudioData(arrayBuffer.slice(0));
    })
    .then((buffer) => {
      notificationSoundBuffer = buffer;
      return buffer;
    })
    .catch((error) => {
      console.debug('Notification sound decode failed', error);
      return null;
    })
    .finally(() => {
      loadingPromise = null;
    });

  return loadingPromise;
};

const playBuffer = (ctx: BrowserAudioContext, buffer: AudioBuffer, gainValue = 1) => {
  const source = ctx.createBufferSource();
  source.buffer = buffer;

  const gainNode = ctx.createGain();
  gainNode.gain.value = gainValue;

  source.connect(gainNode);
  gainNode.connect(ctx.destination);
  source.start(0);
};

export const isNotificationSoundEnabled = (): boolean => {
  if (typeof window === 'undefined') return false;
  return window.localStorage.getItem(NOTIFICATION_SOUND_ENABLED_KEY) === 'true';
};

const setNotificationSoundEnabled = (enabled: boolean) => {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(NOTIFICATION_SOUND_ENABLED_KEY, enabled ? 'true' : 'false');
};

export const enableNotificationSound = async (): Promise<boolean> => {
  const ctx = getAudioContext();
  if (!ctx) return false;

  try {
    if (ctx.state !== 'running') {
      await ctx.resume();
    }
    if (ctx.state !== 'running') {
      return false;
    }

    const buffer = await loadNotificationSound(ctx);
    if (!buffer) {
      return false;
    }

    setNotificationSoundEnabled(true);
    playBuffer(ctx, buffer, 0.85);
    return true;
  } catch (error) {
    console.debug('Enable notification sound failed', error);
    return false;
  }
};

export const primeNotificationAudio = async (): Promise<boolean> => {
  if (!isNotificationSoundEnabled()) return false;

  const ctx = getAudioContext();
  if (!ctx) return false;

  try {
    if (ctx.state !== 'running') {
      await ctx.resume();
    }
    if (ctx.state !== 'running') {
      return false;
    }

    const buffer = await loadNotificationSound(ctx);
    return !!buffer;
  } catch (error) {
    console.debug('Notification audio resume failed', error);
    return false;
  }
};

export const playNotificationSound = async (): Promise<boolean> => {
  if (!isNotificationSoundEnabled()) return false;

  const ctx = getAudioContext();
  if (!ctx) return false;

  try {
    if (ctx.state !== 'running') {
      await ctx.resume();
    }
    if (ctx.state !== 'running') {
      return false;
    }

    const buffer = await loadNotificationSound(ctx);
    if (!buffer) {
      return false;
    }

    playBuffer(ctx, buffer, 1);
    return true;
  } catch (error) {
    console.debug('Notification sound playback failed', error);
    return false;
  }
};
