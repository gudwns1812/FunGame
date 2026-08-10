import { useEffect } from 'react';
import { isSoundName, playSound } from '../utils/sound';

const SOUND_ATTRIBUTE = 'data-sound';
const SILENT_SOUND = 'none';
const SOUND_TARGET_SELECTOR = `button, [${SOUND_ATTRIBUTE}]`;

export const useButtonClickSound = () => {
  useEffect(() => {
    const playSoundOfClickTarget = (event: MouseEvent) => {
      const target = event.target as Element | null;
      const clicked = target?.closest(SOUND_TARGET_SELECTOR);
      if (!clicked) return;

      const requestedSound = clicked.getAttribute(SOUND_ATTRIBUTE);
      if (requestedSound === SILENT_SOUND) return;

      if (isSoundName(requestedSound)) {
        playSound(requestedSound);
        return;
      }

      if (clicked.tagName === 'BUTTON') {
        playSound('buttonClick');
      }
    };

    document.addEventListener('click', playSoundOfClickTarget, true);
    return () => document.removeEventListener('click', playSoundOfClickTarget, true);
  }, []);
};
