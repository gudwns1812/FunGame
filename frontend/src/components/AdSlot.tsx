import React, { useEffect, useRef } from 'react';
import { ADSENSE_CLIENT } from '../utils/adsense';

const ADSENSE_SCRIPT_SRC = `https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${ADSENSE_CLIENT}`;

declare global {
  interface Window {
    adsbygoogle?: Record<string, unknown>[];
  }
}

const loadAdSenseLibraryOnce = () => {
  if (document.querySelector(`script[src="${ADSENSE_SCRIPT_SRC}"]`)) {
    return;
  }

  const script = document.createElement('script');
  script.async = true;
  script.src = ADSENSE_SCRIPT_SRC;
  script.crossOrigin = 'anonymous';
  document.head.appendChild(script);
};

interface AdSlotProps {
  slot: string;
  format?: string;
  labeled?: boolean;
  className?: string;
}

const AdSlot: React.FC<AdSlotProps> = ({ slot, format = 'auto', labeled = false, className }) => {
  const insRef = useRef<HTMLModElement>(null);

  useEffect(() => {
    if (!slot) {
      return;
    }

    loadAdSenseLibraryOnce();

    const ins = insRef.current;
    if (!ins || ins.dataset.adsbygoogleStatus) {
      return;
    }

    window.adsbygoogle = window.adsbygoogle || [];
    window.adsbygoogle.push({});
  }, [slot]);

  if (!slot) {
    return null;
  }

  return (
    <div className={className}>
      {labeled && <p className="px-label mb-1">광고</p>}
      <ins
        ref={insRef}
        className="adsbygoogle block"
        style={{ display: 'block' }}
        data-ad-client={ADSENSE_CLIENT}
        data-ad-slot={slot}
        data-ad-format={format}
        data-full-width-responsive="true"
      />
    </div>
  );
};

export default AdSlot;
