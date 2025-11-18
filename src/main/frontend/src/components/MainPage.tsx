import { Box, Button, TextField } from "@mui/material";
import React, { useEffect, useState, useRef } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import axios from "axios";

var stompClient: any = null;
const PICKUP_URL = "http://localhost:8081/api/queue";

const MainPage = (props: any) => {
  const [message, setMessage] = useState("");
  const [privateChats, setPrivateChats] = useState<
    Map<string, PayloadMessage[]>
  >(new Map());

  const stompClientRef = useRef<Client | null>(null);

  useEffect(() => {
    let sock = new SockJS("http://localhost:8081/ws");
    stompClient = new Client({
      webSocketFactory: () => sock,
      reconnectDelay: 5000,
      debug: (str) => console.log(str),
      onConnect: () => {
        console.log("Connected to WebSocket");
        stompClient.subscribe("/chatroom/1", onPublicMessageReceived);
      },
      onStompError: (frame) => {
        console.error("Broker reported error: " + frame.headers["message"]);
        console.error("Additional details: " + frame.body);
      },
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
    };
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
        date: new Date(),
      };

      client.publish({
        destination: "/app/message",
        body: JSON.stringify(payloadMessage),
      });

      setMessage("");
    }
  }

  async function pickupMessages() {
    const params = new URLSearchParams([["userId", props.user.userId]]);

    try {
      const result = await axios.get<PayloadMessage[]>(PICKUP_URL, { params });

      result.data.forEach((msg) => {
        const chatId = msg.receiverChatRoomId;
        console.log("Chat id is: " + chatId);

        if (!privateChats.has(chatId)) {
          privateChats.set(chatId, []);
          console.log("Creating new private chat");
        }

        console.log("Adding message: " + msg.content);
        privateChats.get(chatId)!.push(msg);

        setPrivateChats(new Map(privateChats));
      });
    } catch (e) {
      console.log("Error", e);
    }
  }

  function logout(e: any) {
    // Unsubscribe from all active subscriptions (if needed)
    if (stompClientRef.current && stompClientRef.current.active) {
      stompClientRef.current.deactivate(); // disconnect
      console.log("Disconnected from WebSocket");
    }
    props.setUserToken("");
  }

  /*
<Box>
              {[...props.privateChats.get(props.activeChat).map((msg, index) => (
                <Box key={index} sx={{ paddingBottom: '10px', width: '100%', overflow: 'auto' }}>
                  {msg.chatUser.userId === props.user.userId ? (
                    <Box sx={{ padding: '10px', float: 'right', textAlign: 'left', backgroundColor: '#057eff', borderRadius: '15px', maxWidth: '75%' }}>
                      {msg.content}
                    </Box>
                  ) : (
                    <Box sx={{ padding: '10px', float: 'left', textAlign: 'left', backgroundColor: '#39393c', borderRadius: '15px', maxWidth: '75%' }}>
                      {msg.content}
                    </Box>
                  )}
                </Box>
*/

  return (
    <div>
      <h2>Hello {props.user.username}</h2>
      <TextField
        label="Type your message here"
        variant="outlined"
        value={message}
        onChange={(e) => setMessage(e.target.value)}
      />
      <Button variant="contained" onClick={sendMessage}>
        Send
      </Button>
      <Box>
        {(privateChats.get("1") ?? []).map((msg, index) => (
          <Box key={index}>
            {msg.senderName === props.user.username ? (
              <Box
                sx={{
                  padding: "10px",
                  marginBottom: '5px',
                  textAlign: "left",
                  backgroundColor: "#057eff",
                  borderRadius: "15px",
                  maxWidth: "75%",
                }}
              >
                {msg.content}
              </Box>
            ) : (
              <Box
                sx={{
                  padding: "10px",
                  marginBottom: '5px',
                  textAlign: "left",
                  backgroundColor: "#39393c",
                  borderRadius: "15px",
                  maxWidth: "75%",
                }}
              >
                {msg.content}
              </Box>
            )}
          </Box>
        ))}
      </Box>
      <Button variant="contained" type="submit" onClick={logout}>
        Log Out
      </Button>
    </div>
  );
};

export default MainPage;

export interface PayloadMessage {
  senderName: string;
  receiverName: string;
  receiverChatRoomId: string;
  content: string;
  date: string;
}
