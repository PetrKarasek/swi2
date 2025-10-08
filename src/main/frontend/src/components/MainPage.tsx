import { Button } from "@mui/material";
import React from "react";

const MainPage = (props: any) => {
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
