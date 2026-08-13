import React from 'react';
import { Link } from 'react-router-dom';
import StaticPageLayout, { StaticSection } from '../components/StaticPageLayout';
import AdSlot from '../components/AdSlot';
import { AD_SLOTS } from '../utils/adsense';

const PAGE_TITLE = '게임 방법';
const PAGE_DESCRIPTION =
  'FUNGAME 노래 맞추기의 시작 방법, 음악 퀴즈·CS 퀴즈·행맨의 규칙, 점수 계산 방식, 방 만들기와 친구 초대 방법, 자주 묻는 질문을 정리했습니다.';

const START_STEPS = [
  '회원가입 후 로그인합니다. 아이디, 비밀번호, 닉네임, 이메일만 있으면 됩니다.',
  '방 목록에서 참가하고 싶은 방을 고르거나, 직접 방을 만듭니다.',
  '대기실에서 준비 완료를 누릅니다. 방장은 모두 준비되면 게임을 시작할 수 있습니다.',
  '게임이 끝나면 순위가 나오고, 대기실로 돌아가 다시 시작할 수 있습니다.',
];

const FAQ_ITEMS = [
  {
    question: '이용료가 있나요?',
    answer: '없습니다. 모든 게임 모드를 무료로 이용할 수 있습니다.',
  },
  {
    question: '노래가 들리지 않아요.',
    answer:
      '노래는 유튜브를 통해 재생됩니다. 브라우저의 자동 재생이 차단되어 있거나 기기가 음소거 상태인지 확인해 주세요. 광고 차단 확장 프로그램이 재생을 막는 경우도 있습니다.',
  },
  {
    question: '게임 도중에 나가면 어떻게 되나요?',
    answer:
      '방은 그대로 유지되며 다시 들어오면 이어서 참여할 수 있습니다. 나가 있는 동안의 라운드 점수는 얻을 수 없습니다.',
  },
  {
    question: '정답을 맞혔는데 인정되지 않았어요.',
    answer:
      '정답은 노래 제목을 기준으로 판정합니다. 띄어쓰기나 특수문자 차이는 무시되지만, 부제나 버전 표기가 다르면 오답이 될 수 있습니다.',
  },
  {
    question: '비밀번호를 잊어버렸어요.',
    answer:
      '로그인 화면의 비밀번호 찾기에서 아이디와 가입 이메일을 입력하면 재설정 링크를 메일로 보내드립니다. 링크는 5분 뒤에 만료됩니다.',
  },
  {
    question: '노래 목록에 원하는 곡이 없어요.',
    answer: '곡 목록은 운영자가 카테고리별로 관리하고 있습니다. 문의 메일로 요청해 주시면 검토 후 추가합니다.',
  },
];

const HowToPlayPage: React.FC = () => (
  <StaticPageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION} path="/how-to-play">
    <StaticSection heading="게임 시작하기">
      <ol className="list-decimal pl-4 space-y-2">
        {START_STEPS.map((step) => (
          <li key={step}>{step}</li>
        ))}
      </ol>
    </StaticSection>

    <StaticSection heading="음악 퀴즈 규칙">
      <p>선택한 카테고리에서 노래가 무작위로 출제됩니다. 진행 방식은 다음과 같습니다.</p>
      <ul className="list-disc pl-4 space-y-1.5">
        <li>라운드가 시작되면 노래가 재생되고 30초가 주어집니다.</li>
        <li>정답은 채팅창에 노래 제목을 입력해 제출합니다. 횟수 제한은 없습니다.</li>
        <li>가장 먼저 맞힌 사람이 그 라운드의 점수를 얻고 라운드가 끝납니다.</li>
        <li>시간이 흐르면 가수 이름과 제목 힌트가 공개됩니다.</li>
        <li>아무도 못 맞히겠다면 스킵 투표를 할 수 있습니다. 참가자 다수가 동의하면 다음 라운드로 넘어갑니다.</li>
        <li>라운드 수는 방을 만들 때 10~100 사이에서 정합니다.</li>
      </ul>
    </StaticSection>

    <StaticSection heading="점수 규칙">
      <ul className="list-disc pl-4 space-y-1.5">
        <li>라운드 정답을 가장 먼저 맞힌 사람이 1점을 얻습니다.</li>
        <li>오답에는 감점이 없습니다. 부담 없이 계속 입력해도 됩니다.</li>
        <li>스킵된 라운드는 아무도 점수를 얻지 않습니다.</li>
        <li>최종 순위는 총점이 높은 순이고, 동점이면 닉네임 가나다순으로 정렬됩니다.</li>
        <li>점수는 한 게임 안에서만 유효하며, 게임이 끝나면 초기화됩니다.</li>
      </ul>
    </StaticSection>

    <AdSlot slot={AD_SLOTS.howToPlayMiddle} className="mb-4" />

    <StaticSection heading="CS 퀴즈 규칙">
      <p>
        컴퓨터 과학 상식 문제를 푸는 모드입니다. 진행 방식과 점수 계산은 음악 퀴즈와 같고, 노래 대신 문제가 출제됩니다.
        문제 수는 10~50개 사이에서 정할 수 있습니다.
      </p>
    </StaticSection>

    <StaticSection heading="행맨 규칙">
      <ul className="list-disc pl-4 space-y-1.5">
        <li>숨겨진 영어 단어를 참가자들이 돌아가며 알파벳 한 글자씩 골라 맞힙니다.</li>
        <li>자기 차례에만 글자를 고를 수 있고, 이미 나온 글자는 다시 고를 수 없습니다.</li>
        <li>틀린 글자가 나올 때마다 남은 기회가 하나씩 줄어듭니다. 기회는 총 6번입니다.</li>
        <li>기회를 다 쓰기 전에 단어를 완성하면 참가자 전원이 함께 이깁니다.</li>
        <li>난이도는 1~4단계이고, 숫자가 클수록 어려운 단어가 나옵니다.</li>
        <li>2~6명이 함께 플레이할 수 있으며 한 판으로 끝납니다.</li>
      </ul>
    </StaticSection>

    <StaticSection heading="방 만드는 방법">
      <p>방 목록 화면에서 방 만들기를 누르면 아래 항목을 정할 수 있습니다.</p>
      <ul className="list-disc pl-4 space-y-1.5">
        <li>
          <strong>게임 종류</strong> — 음악 퀴즈, CS 퀴즈, 행맨 중 선택합니다.
        </li>
        <li>
          <strong>카테고리</strong> — 음악 퀴즈에서만 사용합니다. K-POP, POP, 발라드, 랩/힙합, OST 같은 장르나 소속사,
          세대별로 고를 수 있습니다.
        </li>
        <li>
          <strong>라운드 수</strong> — 음악 퀴즈는 10~100, CS 퀴즈는 10~50, 행맨은 1판 고정입니다.
        </li>
        <li>
          <strong>최대 인원</strong> — 음악·CS 퀴즈는 2~12명, 행맨은 2~6명까지 들어올 수 있습니다.
        </li>
      </ul>
      <p>
        방을 만든 사람이 방장이 되며, 대기실에서 설정을 바꾸거나 게임을 시작할 수 있습니다. 방은 게임이 끝나도 사라지지
        않으니 같은 방에서 계속 이어서 놀 수 있습니다.
      </p>
    </StaticSection>

    <StaticSection heading="친구 초대하기">
      <p>
        대기실에서 접속 중인 유저 목록을 열면 지금 사이트에 들어와 있는 사람들이 보입니다. 초대를 보내면 상대방 화면에
        알림이 뜨고, 수락하면 방으로 바로 들어옵니다.
      </p>
    </StaticSection>

    <StaticSection heading="자주 묻는 질문">
      <dl className="space-y-4">
        {FAQ_ITEMS.map((item) => (
          <div key={item.question}>
            <dt className="px-title text-xs mb-1">Q. {item.question}</dt>
            <dd className="leading-relaxed">{item.answer}</dd>
          </div>
        ))}
      </dl>
    </StaticSection>

    <AdSlot slot={AD_SLOTS.howToPlayBottom} className="mb-6" />

    <div className="flex flex-col sm:flex-row gap-3">
      <Link to="/login" className="px-btn px-btn-primary flex-1 py-3">
        게임 시작하기
      </Link>
      <Link to="/" className="px-btn px-btn-paper flex-1 py-3">
        서비스 소개로
      </Link>
    </div>
  </StaticPageLayout>
);

export default HowToPlayPage;
