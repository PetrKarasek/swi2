import { useState } from 'react'
import './App.css'
import { Route, Routes } from 'react-router-dom';
import Home from './components/Home';
import Signup from './components/Signup';
import { UserToken } from './types';

function App() {
  const [user, setUser] = useState<UserToken | null>(getUserToken());

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
      localStorage.setItem('userToken', userToken);
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
        <Route path='/signup' element={<Signup />} />
      </Routes>
    </div>
  )
}

export default App
