import React, { useState } from 'react';
import { ErrorBoundary } from './components/ErrorBoundary';
import { AppShell } from './components/AppShell';
import { RecruitmentDashboard } from './components/RecruitmentDashboard';
import { UserRoleManagement } from './components/UserRoleManagement';
import { CommonErrorPage } from './components/CommonErrorPage';
import {
  Error404Page,
  Error403Page,
  Error401Page,
  Error500Page,
  Error503Page
} from './components/SpecificErrorPages';
import { ForgotPassword } from './components/ForgotPassword';
import { ResetPassword } from './components/ResetPassword';
import { Register } from './components/Register';
import { AccountManagement } from './components/AccountManagement';

export function App() {
  const [activeTab, setActiveTab] = useState('role-management'); // Set role-management as default tab for KN-19 review!
  const [theme, setTheme] = useState('light');
  const [currentErrorPayload, setCurrentErrorPayload] = useState(null);
  const [simulateCrash, setSimulateCrash] = useState(false);

  React.useEffect(() => {
    if (window.location.pathname === '/reset-password') {
      setActiveTab('reset-password');
    }
  }, []);

  const handleTriggerError = (payload) => {
    setCurrentErrorPayload(payload);
    setActiveTab('api-error');
  };

  const handleTriggerSimulatedError = (type) => {
    if (type === 'REACT_CRASH') {
      setSimulateCrash(true);
    }
  };

  const handleRetry = () => {
    setCurrentErrorPayload(null);
    setSimulateCrash(false);
    setActiveTab('role-management');
  };

  if (simulateCrash) {
    // Deliberate throw to test ErrorBoundary (KN-75)
    throw new Error('Giả lập lỗi Javascript Runtime Crash trên giao diện Client (KN-75 Error Boundary Test)');
  }

  const renderContent = () => {
    if (activeTab === 'api-error' && currentErrorPayload) {
      return (
        <CommonErrorPage
          errorPayload={currentErrorPayload}
          onRetry={handleRetry}
        />
      );
    }

    switch (activeTab) {
      case 'role-management':
        return <UserRoleManagement />;

      case 'accounts':
        return <AccountManagement onTriggerError={handleTriggerError} />;

      case 'dashboard':
        return <RecruitmentDashboard onTriggerError={handleTriggerError} />;

      case 'error-403':
        return <Error403Page onRetry={handleRetry} />;

      case 'error-404':
        return <Error404Page onRetry={handleRetry} />;

      case 'error-401':
        return <Error401Page onRetry={handleRetry} />;

      case 'error-500':
        return <Error500Page onRetry={handleRetry} />;

      case 'error-503':
        return <Error503Page onRetry={handleRetry} />;

      case 'forgot-password':
        return <ForgotPassword onBackToLogin={() => setActiveTab('role-management')} />;

      case 'register':
        return <Register onBackToLogin={() => setActiveTab('role-management')} />;

      case 'reset-password':
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');
        const email = urlParams.get('email');
        return <ResetPassword token={token} email={email} onBackToLogin={() => {
          window.history.replaceState({}, document.title, '/');
          setActiveTab('role-management');
        }} />;

      default:
        return <Error404Page onRetry={handleRetry} />;
    }
  };

  return (
    <ErrorBoundary>
      <AppShell
        activeTab={activeTab}
        setActiveTab={(tab) => {
          setCurrentErrorPayload(null);
          setActiveTab(tab);
        }}
        theme={theme}
        setTheme={setTheme}
        onTriggerSimulatedError={handleTriggerSimulatedError}
      >
        {renderContent()}
      </AppShell>
    </ErrorBoundary>
  );
}

export default App;
