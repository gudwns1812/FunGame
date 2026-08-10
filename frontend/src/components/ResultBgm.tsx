import React from 'react';
import ReactPlayer from 'react-player';

const RESULT_BGM_VIDEO_ID = 'RF4ObMKV5cc';
const RESULT_BGM_START_SECONDS = 57;
const RESULT_BGM_URL = `https://www.youtube.com/watch?v=${RESULT_BGM_VIDEO_ID}&t=${RESULT_BGM_START_SECONDS}s`;

const ResultBgm: React.FC = () => (
  <div className="hidden">
    <ReactPlayer
      src={RESULT_BGM_URL}
      playing={true}
      controls={false}
      width={0}
      height={0}
      onError={(e) => console.error('[ResultBgm] Error:', e)}
    />
  </div>
);

export default ResultBgm;
