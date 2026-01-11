import { useState, useEffect } from 'react'
import './App.css'
import { Route, Routes } from 'react-router-dom';
import Home from './components/Home';
import Signup from './components/Signup';
import { UserToken } from './types';

function App() {
  const [user, setUser] = useState<UserToken | null>(getUserToken());

  // Check backend status and track restarts
  useEffect(() => {
    const checkBackendStatus = async () => {
      try {
        const response = await fetch('http://localhost:8081/users');
        if (response.ok) {
          console.log("Backend is available");
          const now = Date.now().toString();
          const lastBackendStart = localStorage.getItem('lastBackendStart');
          
          // If backend was restarted (more than 5 minutes since last check), clear old data
          if (lastBackendStart && (parseInt(now) - parseInt(lastBackendStart)) > 300000) {
            console.log("Backend restarted, clearing old session data");
            localStorage.removeItem('dm_conversations');
            localStorage.removeItem('unreadPublicMessages');
            localStorage.removeItem('unreadDMs');
            localStorage.removeItem('pendingMessages');
          }
          
          localStorage.setItem('lastBackendStart', now);
        }
      } catch (error) {
        console.log("Backend not available yet");
      }
    };

    checkBackendStatus();
  }, []);

  function getUserToken(): UserToken | null {
    const userTokenStr = localStorage.getItem('userToken');
    if (!userTokenStr || userTokenStr === '""') return null;
    
    try {
      return JSON.parse(userTokenStr);
    } catch {
      return null;
    }
  }

  function setUserToken(userToken: UserToken | null | string) {
    if (typeof userToken === 'string' || userToken === null) {
      localStorage.setItem('userToken', userToken || '');
      setUser(null);
    } else {
      localStorage.setItem('userToken', JSON.stringify(userToken));
      setUser(userToken);
    }
  }

  return (
    <div className="App">
      <Routes>
        <Route path='/' element={<Home user={user} setUserToken={setUserToken} />} />
        <Route path='/signup' element={<Signup setUserToken={setUserToken} />} />
      </Routes>
    </div>
  )
}

export default App
