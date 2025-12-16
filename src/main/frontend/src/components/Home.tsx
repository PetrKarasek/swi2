import React from "react";
import { Box, Container, Typography, Paper, Grid } from "@mui/material";
import Login from "./Login";
import MainPage from "./MainPage";
import { UserToken } from "../types";

interface HomeProps {
  user: UserToken | null;
  setUserToken: (userToken: UserToken | null | string) => void;
}

const Home: React.FC<HomeProps> = (props) => {
  const isLoggedIn = !!props.user;

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
      }}
    >
      {!isLoggedIn ? (
        <>
          <Box
            component="header"
            sx={{
              py: 2,
              borderBottom: "1px solid rgba(255,255,255,0.08)",
              backdropFilter: "blur(10px)",
            }}
          >
            <Container maxWidth="lg">
              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  gap: 2,
                }}
              >
                <Typography
                  variant="h5"
                  sx={{
                    fontWeight: 700,
                    letterSpacing: "0.08em",
                    textTransform: "uppercase",
                  }}
                >
                  SWI2 Chat
                </Typography>
                <Typography variant="body2" sx={{ opacity: 0.8 }}>
                  Jednoduchá víceuživatelská chatovací aplikace
                </Typography>
              </Box>
            </Container>
          </Box>

          <Container
            maxWidth="lg"
            sx={{
              flex: 1,
              display: "flex",
              alignItems: "center",
              py: { xs: 4, md: 8 },
            }}
          >
            <Grid container spacing={4} alignItems="stretch">
              <Grid item xs={12} md={6}>
                <Typography
                  variant="h3"
                  sx={{
                    fontWeight: 700,
                    mb: 2,
                    lineHeight: 1.1,
                  }}
                >
                  Vstup do hlavní místnosti
                </Typography>
                <Typography
                  variant="h6"
                  sx={{ mb: 3, maxWidth: 480, opacity: 0.9 }}
                >
                  Přihlas se jedním účtem a chatuj v hlavní veřejné místnosti se
                  všemi ostatními uživateli. Zprávy pro odhlášené uživatele
                  bezpečně čekají ve frontě.
                </Typography>
                <Paper
                  sx={{
                    p: 2.5,
                    background:
                      "linear-gradient(135deg, rgba(255,255,255,0.06), rgba(255,255,255,0.02))",
                    borderRadius: 3,
                    border: "1px solid rgba(255,255,255,0.18)",
                  }}
                >
                  <Typography variant="subtitle1" sx={{ mb: 1.5 }}>
                    Jak to funguje
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9, mb: 0.5 }}>
                    • Webová aplikace (React) nad společnou databází
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9, mb: 0.5 }}>
                    • Jedna databáze uživatelů pro všechny klienty
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    • Zprávy pro nepřihlášené uživatele se ukládají do fronty a
                    doručí se po přihlášení
                  </Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} md={6}>
                <Paper
                  elevation={8}
                  sx={{
                    p: { xs: 3, md: 4 },
                    borderRadius: 3,
                    backgroundColor: "#0f172a",
                    border: "1px solid rgba(148, 163, 184, 0.35)",
                  }}
                >
                  <Login setUserToken={props.setUserToken} />
                </Paper>
              </Grid>
            </Grid>
          </Container>
        </>
      ) : (
        <MainPage user={props.user} setUserToken={props.setUserToken} />
      )}
    </Box>
  );
};

export default Home;