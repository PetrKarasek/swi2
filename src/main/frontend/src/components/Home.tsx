import React from 'react'
import Login from './Login'
import MainPage from './MainPage'

const Home = (props: any) => {
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