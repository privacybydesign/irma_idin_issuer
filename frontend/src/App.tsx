import React from 'react';
import {
  BrowserRouter as Router,
  Routes,
  Route,
} from 'react-router-dom';
import IndexPage from './pages/Index';
import EnrollPage from './pages/Enroll';
import DonePage from './pages/Done';
import ErrorPage from './pages/Error';

export default function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<IndexPage />} />
        <Route path="/enroll" element={<EnrollPage />} />
        <Route path="/done" element={<DonePage />} />
        <Route path="/error" element={<ErrorPage />} />
      </Routes>
    </Router>
  );
}
