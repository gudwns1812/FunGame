import React from 'react';
import { NavLink } from 'react-router-dom';

interface MyPageNavProps {
  isAdmin: boolean;
}

const ITEMS = [
  { to: '/mypage', label: '내 정보', end: true },
  { to: '/mypage/reports', label: '내 문의', end: false },
];

const ADMIN_ITEM = { to: '/mypage/inquiries', label: '문의 관리', end: false };

const MyPageNav: React.FC<MyPageNavProps> = ({ isAdmin }) => {
  const items = isAdmin ? [...ITEMS, ADMIN_ITEM] : ITEMS;

  return (
    <nav className="px-card shrink-0 p-2 flex sm:flex-col gap-2 sm:w-44 overflow-x-auto">
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          className={({ isActive }) =>
            `px-btn px-btn-sm whitespace-nowrap sm:w-full sm:justify-start ${
              isActive ? 'px-btn-primary' : 'px-btn-paper'
            }`
          }>
          {item.label}
        </NavLink>
      ))}
    </nav>
  );
};

export default MyPageNav;
