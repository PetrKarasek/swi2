import { Box, Button, Chip, Divider, TextField } from "@mui/material";
import axios from "axios";
import React, { useState } from "react";
import { Link } from "react-router-dom";

const LOGIN_TOKEN_URL = "http://localhost:8081/login";

const Login = (props: any) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  function login(e: any) {
    e.preventDefault();
    
    const userCredentials = {
      username: username,
      password: password
    };

    axios.post(LOGIN_TOKEN_URL, userCredentials)
      .then(response => {
        props.setUserToken(response.data);
      })
      .catch(error => {
        try {
          console.log(error.response.data);
          // výpis na stránku
        } catch (e) {
          console.log(e);
          // Cannot access authentication server
        }
      });
  }

  return (
    <div>
      <h2>Please Log in</h2>
      <form onSubmit={login}>
        <Box>
          <TextField
            placeholder="Username"
            onChange={(e) => setUsername(e.target.value)}
          />
        </Box>
        <Box>
          <TextField
            placeholder="Password"
            type="password"
            onChange={(e) => setPassword(e.target.value)}
          />
        </Box>
        <Box>
          <Button type="submit" variant="contained">
            Log in
          </Button>
        </Box>
      </form>
      <Divider>
        <Chip label="OR" />
      </Divider>
      <Link to="/signup">
        <Button>Sign up now</Button>
      </Link>
    </div>
  );
};

export default Login;
