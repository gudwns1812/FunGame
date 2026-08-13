import React from 'react';
import StaticPageLayout, { StaticSection } from '../components/StaticPageLayout';

const PAGE_TITLE = '개인정보처리방침';
const PAGE_DESCRIPTION =
  'FUNGAME이 수집하는 개인정보 항목과 이용 목적, 보유 기간, 쿠키와 광고 기술의 사용, 이용자의 권리를 안내합니다.';

const CONTACT_EMAIL = 'gudwns1812@naver.com';
const EFFECTIVE_DATE = '2026년 8월 13일';

const ExternalLink: React.FC<{ href: string; children: React.ReactNode }> = ({ href, children }) => (
  <a href={href} target="_blank" rel="noopener noreferrer" className="text-cherry underline break-all">
    {children}
  </a>
);

const PrivacyPage: React.FC = () => (
  <StaticPageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION} path="/privacy">
    <StaticSection heading="1. 총칙">
      <p>
        FUNGAME(이하 "서비스")은 이용자의 개인정보를 소중하게 생각하며, 개인정보 보호법 등 관련 법령을 준수합니다. 본
        방침은 서비스가 어떤 정보를 어떤 목적으로 수집하고 어떻게 관리하는지를 안내합니다.
      </p>
    </StaticSection>

    <StaticSection heading="2. 수집하는 개인정보 항목">
      <p>서비스는 회원가입과 게임 이용에 필요한 최소한의 정보만 수집합니다.</p>
      <ul className="list-disc pl-4 space-y-1.5">
        <li>
          <strong>회원가입 시 직접 입력</strong> — 아이디, 비밀번호, 닉네임, 이메일 주소
        </li>
        <li>
          <strong>서비스 이용 중 자동 생성</strong> — 접속 IP 주소, 접속 일시, 브라우저 및 기기 정보, 서비스 이용
          기록(방 참가 이력, 게임 진행 기록), 게임 중 입력한 채팅 메시지
        </li>
      </ul>
      <p>
        비밀번호는 단방향 암호화되어 저장되며, 운영자를 포함한 누구도 원래 값을 확인할 수 없습니다. 서비스는 이름,
        생년월일, 전화번호, 결제 정보 등 민감정보나 고유식별정보를 수집하지 않습니다.
      </p>
    </StaticSection>

    <StaticSection heading="3. 개인정보의 이용 목적">
      <ul className="list-disc pl-4 space-y-1.5">
        <li>회원 식별과 로그인 유지, 중복 가입 방지</li>
        <li>닉네임 표시 등 게임 진행에 필요한 기능 제공</li>
        <li>비밀번호 재설정 링크 발송 등 계정 관리</li>
        <li>부정 이용 방지, 서비스 장애 대응, 이용 통계 분석을 통한 서비스 개선</li>
        <li>맞춤형 광고를 포함한 광고 게재</li>
      </ul>
    </StaticSection>

    <StaticSection heading="4. 보유 및 이용 기간">
      <p>
        수집한 개인정보는 회원 탈퇴 시까지 보유하며, 탈퇴 요청을 받으면 지체 없이 파기합니다. 다만 관계 법령에서 일정
        기간 보존을 요구하는 경우에는 해당 기간 동안 보관합니다. 비밀번호 재설정 토큰은 발급 후 5분이 지나면 만료되어
        사용할 수 없습니다.
      </p>
      <p>
        전자적 파일 형태의 정보는 복구할 수 없는 방법으로 삭제하며, 게임 진행 기록과 채팅 로그는 게임 종료 또는 방 삭제
        시 함께 정리됩니다.
      </p>
    </StaticSection>

    <StaticSection heading="5. 쿠키의 사용">
      <p>
        서비스는 로그인 상태를 유지하기 위해 세션 쿠키를 사용합니다. 또한 브라우저의 로컬 저장소에 닉네임과 진행 중인
        게임 상태를 저장하여, 새로고침하더라도 게임을 이어서 진행할 수 있도록 합니다.
      </p>
      <p>
        이용자는 브라우저 설정에서 쿠키 저장을 거부할 수 있습니다. 다만 로그인에 필요한 쿠키를 차단하면 서비스를 정상적으로
        이용할 수 없습니다.
      </p>
    </StaticSection>

    <StaticSection heading="6. 광고 및 제3자 기술의 사용">
      <p>
        서비스는 운영 비용을 마련하기 위해 Google이 제공하는 광고 서비스인 Google AdSense를 사용합니다. 이와 관련하여
        다음 사항을 안내드립니다.
      </p>
      <ul className="list-disc pl-4 space-y-1.5">
        <li>
          Google을 포함한 제3자 광고 공급업체는 이용자가 이 사이트나 다른 사이트를 방문한 기록을 바탕으로 광고를 게재하기
          위해 쿠키를 사용합니다.
        </li>
        <li>
          이 과정에서 이용자의 IP 주소, 브라우저 및 기기 정보, 광고 식별자, 페이지 조회 기록 등이 광고 공급업체에 의해
          수집·이용될 수 있습니다.
        </li>
        <li>
          Google은 광고 쿠키를 사용하여 이 사이트와 다른 사이트에서의 방문 기록을 기반으로 맞춤 광고를 게재합니다.
        </li>
        <li>
          이용자는 <ExternalLink href="https://www.google.com/settings/ads">Google 광고 설정</ExternalLink>에서 맞춤 광고
          사용을 해제할 수 있습니다. 제3자 광고 공급업체의 맞춤 광고는{' '}
          <ExternalLink href="https://www.aboutads.info/choices/">www.aboutads.info/choices</ExternalLink>에서 일괄
          해제할 수 있습니다.
        </li>
        <li>
          Google이 데이터를 처리하는 방식은{' '}
          <ExternalLink href="https://policies.google.com/technologies/partner-sites">
            Google의 파트너 사이트 데이터 사용 방침
          </ExternalLink>
          에서 확인할 수 있습니다.
        </li>
      </ul>
      <p>
        또한 음악 퀴즈의 노래 재생에는 YouTube 플레이어가 사용됩니다. 재생 중 YouTube 및 Google이 이용자의 기기 정보와
        시청 기록을 수집할 수 있으며, 자세한 내용은{' '}
        <ExternalLink href="https://policies.google.com/privacy">Google 개인정보처리방침</ExternalLink>을 참고해 주세요.
      </p>
    </StaticSection>

    <StaticSection heading="7. 개인정보의 제3자 제공">
      <p>
        서비스는 이용자의 개인정보를 제3자에게 판매하거나 제공하지 않습니다. 다만 법령에 근거하여 수사기관이 적법한
        절차에 따라 요청하는 경우에는 예외로 합니다. 광고 및 노래 재생을 위해 Google 등 제3자 기술이 사용되는 범위는
        6항에 안내한 바와 같습니다.
      </p>
    </StaticSection>

    <StaticSection heading="8. 이용자의 권리와 행사 방법">
      <p>
        이용자는 언제든지 자신의 개인정보를 조회하거나 수정할 수 있고, 회원 탈퇴를 통해 수집·이용 동의를 철회할 수
        있습니다. 닉네임과 비밀번호는 마이페이지에서 직접 변경할 수 있으며, 그 밖의 열람·정정·삭제 요청은 아래 문의처로
        연락해 주시면 지체 없이 처리하겠습니다.
      </p>
    </StaticSection>

    <StaticSection heading="9. 아동의 개인정보">
      <p>
        서비스는 만 14세 미만 아동의 회원가입을 받지 않습니다. 만 14세 미만 아동의 개인정보가 수집된 사실을 확인하면
        해당 정보를 즉시 삭제합니다.
      </p>
    </StaticSection>

    <StaticSection heading="10. 개인정보의 안전성 확보 조치">
      <ul className="list-disc pl-4 space-y-1.5">
        <li>비밀번호는 단방향 암호화하여 저장합니다.</li>
        <li>통신 구간은 HTTPS로 암호화합니다.</li>
        <li>개인정보에 접근할 수 있는 권한을 운영에 필요한 최소한의 인원으로 제한합니다.</li>
      </ul>
    </StaticSection>

    <StaticSection heading="11. 문의처">
      <p>
        개인정보 처리에 관한 문의, 불만 처리, 피해 구제는 아래로 연락해 주세요.
      </p>
      <p>
        이메일: <ExternalLink href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</ExternalLink>
      </p>
      <p>
        개인정보 침해에 대한 신고나 상담이 필요하시면 개인정보침해신고센터(privacy.kisa.or.kr, 국번없이 118),
        개인정보 분쟁조정위원회(kopico.go.kr, 1833-6972)에 문의하실 수 있습니다.
      </p>
    </StaticSection>

    <StaticSection heading="12. 방침의 변경">
      <p>
        본 방침의 내용이 변경되는 경우 변경 사항을 이 페이지에 게시합니다. 중요한 변경이 있을 때에는 시행일 최소 7일
        전부터 공지합니다.
      </p>
      <p className="px-label">시행일: {EFFECTIVE_DATE}</p>
    </StaticSection>
  </StaticPageLayout>
);

export default PrivacyPage;
