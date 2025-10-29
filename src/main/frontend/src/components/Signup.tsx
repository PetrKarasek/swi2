import { Box, Button, Chip, Divider, TextField } from "@mui/material";
import axios from "axios";
import React, { useState } from "react";
import { Link } from "react-router-dom";

const SIGNUP_TOKEN_URL = "http://localhost:8081/signup";

const Signup = (props: any) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  function signup(e: any) {
    e.preventDefault();

    const userDTO = {
      username: username,
      password: password
    };

    axios.post(SIGNUP_TOKEN_URL, userDTO)
      .then(response => {
        console.log(response.data);
      })
      .catch(error => {
        try {
          console.log(error.response.data);
        } catch (e) {
          console.log(e);
          // Cannot access registration server
        }
      });
  }

  return (
    <div>
      <h2>Please Sign up</h2>
      <form onSubmit={signup}>
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
            Sign up
          </Button>
        </Box>
      </form>
      <Divider>
        <Chip label="OR" />
      </Divider>
      <Link to="/">
        <Button>Log in</Button>
      </Link>
    </div>
  );
};

export default Signup;
