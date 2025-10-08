import { Box, Button, Chip, Divider, TextField } from "@mui/material";
import React, { useState } from "react";
import { Link } from "react-router-dom";

const Signup = (props: any) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  function signup(e: any) {
    e.preventDefault();
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
