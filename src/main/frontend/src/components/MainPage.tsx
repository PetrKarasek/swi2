import { Button } from "@mui/material";
import React, { useEffect, useState } from "react";
import SockJS from 'sockjs-client';
import { over } from 'stompjs';

var stompClient: any = null;
const PICKUP_URL = "http://localhost:8081/api/queue";

const MainPage = (props: any) => {

  const [message, setMessage] = useState("");

  useEffect(() => {
    let sock = new SockJS("http://localhost:8081/ws");
    stompClient = over(sock);
    stompClient.connect({}, onConnected, onError);

    return () => {
      if (stompClient) {
        stompClient.disconnect();
      }
    }
  }, []);

  function onConnected() {
    stompClient.subscribe("/chatroom/1", onPublicMessageReceived);
  }

  function onError(e: any) {
    console.log("WebSocket connection error: " + e);
  }

  function onPublicMessageReceived(payload: any) {
    pickupMessages();
  }

  function sendMessage() {
    // poslat zprávu přes websocket na backend
  }

  function pickupMessages() {
    // vyzvednout zprávy z fronty (rabbit)
  }

  function logout(e: any) {
    props.setUserToken("");
  }

  return (
    <div>
      <h2>Hello {props.user.username}</h2>
      <Button variant="contained" type="submit" onClick={logout}>
        Log Out
      </Button>
    </div>
  );
};

export default MainPage;
