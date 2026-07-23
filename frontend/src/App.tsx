import { BrowserRouter, Routes, Route } from 'react-router-dom';

import IndexPage from './pages/Index';
import EnrollPage from './pages/Enroll';
import ErrorPage from './pages/Error';
import SuccessPage from './pages/Success';

import './i18n';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<IndexPage />} />
        <Route path="/enroll" element={<EnrollPage />} />
        <Route path="/success" element={<SuccessPage />} />
        <Route path="/error" element={<ErrorPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
