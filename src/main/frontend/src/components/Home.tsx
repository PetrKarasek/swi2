import React from 'react'
import Login from './Login'
import MainPage from './MainPage'
import { UserToken } from '../types';

interface HomeProps {
  user: UserToken | null;
  setUserToken: (userToken: UserToken | null | string) => void;
}

const Home: React.FC<HomeProps> = (props) => {
  return (
    <div>
        {!props.user ? (
            <Login setUserToken={props.setUserToken} />
        ) : (
            <MainPage user={props.user} setUserToken={props.setUserToken} />
        )}
    </div>
  )
}

export default Home