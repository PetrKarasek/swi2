import { Box, Button, Chip, Divider, TextField } from "@mui/material";
import React, { useState } from "react";
import { Link } from "react-router-dom";

const Login = (props: any) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  function login(e: any) {
    e.preventDefault();
    props.setUserToken(username);
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
