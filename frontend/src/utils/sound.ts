const SOUND_SOURCES = {
  buttonClick: '/sound/button_click.mp3',
  correctAnswer: '/sound/corret_answer.mp3',
  roomCreate: '/sound/room_create.mp3',
} as const;

export type SoundName = keyof typeof SOUND_SOURCES;

const SOUND_VOLUME = 0.5;

const preloadedSounds = new Map<SoundName, HTMLAudioElement>();

const preload = (name: SoundName): HTMLAudioElement | null => {
  if (typeof Audio === 'undefined') return null;

  const preloaded = preloadedSounds.get(name);
  if (preloaded) return preloaded;

  const audio = new Audio(SOUND_SOURCES[name]);
  audio.preload = 'auto';
  preloadedSounds.set(name, audio);
  return audio;
};

const ignorePlaybackFailure = () => undefined;

export const isSoundName = (value: string | null | undefined): value is SoundName =>
  value !== null && value !== undefined && value in SOUND_SOURCES;

export const playSound = (name: SoundName) => {
  const preloadedSound = preload(name);
  if (!preloadedSound) return;

  const overlappablePlayback = preloadedSound.cloneNode() as HTMLAudioElement;
  overlappablePlayback.volume = SOUND_VOLUME;

  Promise.resolve()
    .then(() => overlappablePlayback.play())
    .catch(ignorePlaybackFailure);
};
