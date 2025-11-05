import { Button, TextField } from "@mui/material";
import React, { useEffect, useState, useRef } from "react";
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import axios from "axios";

var stompClient: any = null;
const PICKUP_URL = "http://localhost:8081/api/queue";

const MainPage = (props: any) => {

  const [message, setMessage] = useState("");
  const stompClientRef = useRef<Client | null>(null);

  useEffect(() => {
    let sock = new SockJS("http://localhost:8081/ws");
    stompClient = new Client({
      webSocketFactory: () => sock,
      reconnectDelay: 5000,
      debug: str => console.log(str),
      onConnect: () => {
        console.log("Connected to WebSocket");
        stompClient.subscribe("/chatroom/1", onPublicMessageReceived);
      },
      onStompError: (frame) => {
        console.error("Broker reported error: " + frame.headers["message"]);
        console.error("Additional details: " + frame.body);
      }
    });

    stompClientRef.current = stompClient;
    stompClient.activate();

    pickupMessages();

    return () => {
      // Disconnect when components unmount
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
        stompClientRef.current = null;
      }
    }
  }, []);

  function onPublicMessageReceived(payload: any) {
    let payloadData = JSON.parse(payload.body);
    console.log("Message received from: " + payloadData.senderName);
    pickupMessages();
  }

  function sendMessage() {
    // Send message to backend through WebSocket
    const client = stompClientRef.current;
    if (client && client.connected) {
      const payloadMessage = {
        senderName: props.user.username,
        receiverChatRoomId: 1,
        content: message,
        date: new Date()
      };

      client.publish({
        destination: "/app/message",
        body: JSON.stringify(payloadMessage),
      });

      setMessage("");
    }
  }

  function pickupMessages() {
    axios.get(PICKUP_URL, {
      params: {
        userId: props.user.userId
      }
    }).then(response => {
      console.log(response.data);
    }).catch(error => {
      try {
        console.log(error.response.data);
      } catch (e) {
        console.log("Cannot access back-end server!");
      }
    })
  }

  function logout(e: any) {
    // Unsubscribe from all active subscriptions (if needed)
    if (stompClientRef.current && stompClientRef.current.active) {
      stompClientRef.current.deactivate(); // disconnect
      console.log("Disconnected from WebSocket");
    }
    props.setUserToken("");
  }

  return (
    <div>
      <h2>Hello {props.user.username}</h2>
      <TextField
        label="Type your message here"
        variant="outlined"
        value={message}
        onChange={(e) => setMessage(e.target.value)}
      />
      <Button
        variant="contained"
        onClick={sendMessage}
      >Send
      </Button>
      <Button variant="contained" type="submit" onClick={logout}>
        Log Out
      </Button>
    </div>
  );
};

export default MainPage;
