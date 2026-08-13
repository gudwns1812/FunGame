import React from 'react';
import { Link } from 'react-router-dom';
import StaticPageLayout, { StaticSection } from '../components/StaticPageLayout';

const PAGE_TITLE = '이용약관';
const PAGE_DESCRIPTION =
  'FUNGAME 서비스의 이용 조건, 회원의 권리와 의무, 금지 행위, 저작권과 책임의 범위를 안내합니다.';

const CONTACT_EMAIL = 'gudwns1812@naver.com';
const EFFECTIVE_DATE = '2026년 8월 13일';

const PROHIBITED_ACTS = [
  '타인의 계정을 무단으로 사용하거나 개인정보를 도용하는 행위',
  '욕설, 혐오 표현, 성적 표현 등 다른 이용자에게 불쾌감을 주는 내용을 채팅이나 닉네임에 사용하는 행위',
  '자동화 프로그램이나 비정상적인 방법으로 게임 결과를 조작하는 행위',
  '서비스의 정상적인 운영을 방해하거나 서버에 과도한 부하를 일으키는 행위',
  '서비스에 게시된 콘텐츠를 운영자의 동의 없이 상업적으로 이용하는 행위',
  '법령을 위반하거나 제3자의 권리를 침해하는 행위',
];

const TermsPage: React.FC = () => (
  <StaticPageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION} path="/terms">
    <StaticSection heading="제1조 (목적)">
      <p>
        본 약관은 FUNGAME(이하 "서비스")이 제공하는 온라인 퀴즈 게임 서비스의 이용 조건과 절차, 이용자와 운영자의 권리와
        의무를 정하는 것을 목적으로 합니다.
      </p>
    </StaticSection>

    <StaticSection heading="제2조 (용어의 정의)">
      <ul className="list-disc pl-4 space-y-1.5">
        <li>
          <strong>이용자</strong> — 본 약관에 따라 서비스를 이용하는 모든 사람을 말합니다.
        </li>
        <li>
          <strong>회원</strong> — 서비스에 가입하여 계정을 부여받은 이용자를 말합니다.
        </li>
        <li>
          <strong>방</strong> — 회원이 생성하여 다른 회원과 함께 게임을 진행하는 공간을 말합니다.
        </li>
        <li>
          <strong>콘텐츠</strong> — 서비스에서 제공하는 문제, 정답, 화면 구성 등 일체의 자료를 말합니다.
        </li>
      </ul>
    </StaticSection>

    <StaticSection heading="제3조 (약관의 효력과 변경)">
      <p>
        본 약관은 서비스 화면에 게시함으로써 효력이 발생합니다. 운영자는 필요한 경우 관련 법령을 위반하지 않는 범위에서
        약관을 변경할 수 있으며, 변경된 약관은 시행일 7일 전부터 이 페이지에 게시합니다. 이용자가 변경된 약관에 동의하지
        않는 경우 회원 탈퇴를 통해 이용 계약을 해지할 수 있습니다.
      </p>
    </StaticSection>

    <StaticSection heading="제4조 (이용 계약의 성립)">
      <p>
        이용 계약은 이용자가 본 약관에 동의하고 회원가입을 신청한 뒤 운영자가 이를 승낙함으로써 성립합니다. 운영자는
        다음의 경우 가입을 승낙하지 않거나 사후에 계약을 해지할 수 있습니다.
      </p>
      <ul className="list-disc pl-4 space-y-1.5">
        <li>타인의 명의나 허위 정보를 사용하여 가입한 경우</li>
        <li>이전에 본 약관 위반으로 이용이 제한된 이력이 있는 경우</li>
        <li>만 14세 미만인 경우</li>
      </ul>
    </StaticSection>

    <StaticSection heading="제5조 (계정 관리)">
      <p>
        회원은 자신의 아이디와 비밀번호를 직접 관리할 책임이 있으며, 이를 제3자에게 양도하거나 대여할 수 없습니다.
        계정이 도용된 사실을 알게 된 경우 즉시 운영자에게 알리고 안내에 따라야 합니다.
      </p>
    </StaticSection>

    <StaticSection heading="제6조 (서비스의 제공)">
      <p>
        서비스는 음악 퀴즈, CS 퀴즈, 행맨 등의 게임 기능과 방 생성·참가, 채팅, 친구 초대 기능을 제공합니다. 모든 기능은
        무료로 제공되며, 운영자는 서비스의 내용과 구성을 변경하거나 일부 기능을 추가·중단할 수 있습니다.
      </p>
      <p>
        서비스는 연중무휴 24시간 제공을 원칙으로 하나, 시스템 점검·교체, 설비 장애, 통신 두절, 천재지변 등의 사유가
        있는 경우 일시적으로 중단될 수 있습니다.
      </p>
    </StaticSection>

    <StaticSection heading="제7조 (이용자의 의무)">
      <p>이용자는 다음 행위를 해서는 안 됩니다.</p>
      <ul className="list-disc pl-4 space-y-1.5">
        {PROHIBITED_ACTS.map((act) => (
          <li key={act}>{act}</li>
        ))}
      </ul>
      <p>
        위 행위가 확인되면 운영자는 사전 통보 없이 채팅 제한, 이용 정지, 계정 삭제 등의 조치를 취할 수 있습니다.
      </p>
    </StaticSection>

    <StaticSection heading="제8조 (저작권)">
      <p>
        서비스가 제작한 화면 구성, 문제, 데이터에 대한 저작권은 운영자에게 있습니다. 이용자는 운영자의 사전 동의 없이
        이를 복제, 배포, 상업적으로 이용할 수 없습니다.
      </p>
      <p>
        음악 퀴즈에서 재생되는 음원은 서비스가 직접 보유하거나 배포하지 않으며, YouTube 플레이어를 통해 제공됩니다. 해당
        음원의 저작권은 각 권리자에게 있고, 재생은 YouTube의 서비스 약관에 따릅니다. 권리자께서 특정 콘텐츠의 사용 중단을
        요청하시는 경우 확인 후 즉시 조치하겠습니다.
      </p>
    </StaticSection>

    <StaticSection heading="제9조 (게시물과 채팅)">
      <p>
        이용자가 채팅이나 닉네임으로 입력한 내용에 대한 책임은 작성자에게 있습니다. 운영자는 본 약관이나 법령을 위반하는
        내용을 사전 통보 없이 삭제하거나 노출을 제한할 수 있습니다.
      </p>
    </StaticSection>

    <StaticSection heading="제10조 (광고의 게재)">
      <p>
        운영자는 서비스 운영과 관련하여 화면에 광고를 게재할 수 있습니다. 광고에 포함된 상품이나 서비스는 광고주가
        제공하는 것으로, 이용자가 광고를 통해 진행한 거래에 대해서는 운영자가 책임지지 않습니다. 광고 게재에 따른 정보
        수집에 관해서는{' '}
        <Link to="/privacy" className="text-cherry underline">
          개인정보처리방침
        </Link>
        을 확인해 주세요.
      </p>
    </StaticSection>

    <StaticSection heading="제11조 (책임의 제한)">
      <p>
        운영자는 천재지변, 통신 장애 등 불가항력으로 서비스를 제공할 수 없는 경우 책임을 지지 않습니다. 또한 이용자
        본인의 귀책 사유로 발생한 손해, 이용자 간 분쟁, 이용자가 서비스를 통해 얻은 정보에 대한 신뢰로 발생한 손해에
        대해서도 책임을 지지 않습니다.
      </p>
      <p>
        서비스는 무료로 제공되므로, 서비스 이용과 관련하여 이용자에게 발생한 손해에 대해 관련 법령이 허용하는 범위에서
        책임을 지지 않습니다.
      </p>
    </StaticSection>

    <StaticSection heading="제12조 (이용 계약의 해지)">
      <p>
        회원은 언제든지 탈퇴를 요청하여 이용 계약을 해지할 수 있습니다. 탈퇴 시 계정과 관련된 정보는 개인정보처리방침에
        따라 처리됩니다.
      </p>
    </StaticSection>

    <StaticSection heading="제13조 (준거법과 분쟁 해결)">
      <p>
        본 약관은 대한민국 법령에 따라 해석되며, 서비스 이용과 관련하여 분쟁이 발생한 경우 운영자와 이용자는 원만한
        해결을 위해 성실히 협의합니다. 협의가 이루어지지 않으면 민사소송법에 따른 관할 법원에 소를 제기할 수 있습니다.
      </p>
      <p>
        문의: <a href={`mailto:${CONTACT_EMAIL}`} className="text-cherry underline break-all">{CONTACT_EMAIL}</a>
      </p>
      <p className="px-label">시행일: {EFFECTIVE_DATE}</p>
    </StaticSection>
  </StaticPageLayout>
);

export default TermsPage;
