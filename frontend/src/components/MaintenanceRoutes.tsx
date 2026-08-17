import React from 'react';
import { Route, Routes } from 'react-router-dom';
import MaintenancePage from '../pages/MaintenancePage';
import { PUBLIC_PAGES } from '../publicPages';

const MaintenanceRoutes: React.FC = () => (
  <Routes>
    {PUBLIC_PAGES.map((page) => (
      <Route key={page.path} path={page.path} element={page.element} />
    ))}
    <Route path="*" element={<MaintenancePage />} />
  </Routes>
);

export default MaintenanceRoutes;
