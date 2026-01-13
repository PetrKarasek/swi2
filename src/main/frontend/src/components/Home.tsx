import React from "react";
import { Box, Container, Typography, Paper, Grid } from "@mui/material";
import Login from "./Login";
import MainPage from "./MainPage.tsx";
import MainPagePreview from "./MainPagePreview";
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
        backgroundColor: "#f3f4f6",
        color: "#111827",
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
                  Víceuživatelská chatovací aplikace
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
              <Grid size={{ xs: 12, md: 5 }}>
                <Typography
                  variant="h3"
                  sx={{
                    fontWeight: 700,
                    mb: 2,
                    lineHeight: 1.1,
                    color: "#111827",
                  }}
                >
                  Vstup do hlavní místnosti
                </Typography>
                <Typography
                  variant="h6"
                  sx={{ mb: 3, maxWidth: 480, opacity: 0.9, color: "#374151" }}
                >
                  Po otevření aplikace vidíš hlavní veřejnou místnost. Zprávy
                  mohou číst pouze přihlášení uživatelé, ale rozhraní místnosti
                  je viditelné pro všechny.
                </Typography>
                <Paper
                  sx={{
                    p: 2.5,
                    borderRadius: 3,
                    border: "1px solid #e5e7eb",
                    backgroundColor: "#ffffff",
                  }}
                >
                  <Typography variant="subtitle1" sx={{ mb: 1.5, color: "#111827" }}>
                    Jak to funguje
                  </Typography>
                  <Typography variant="body2" sx={{ mb: 0.5, color: "#374151" }}>
                    • Webová i desktopová aplikace nad společnou databází
                  </Typography>
                  <Typography variant="body2" sx={{ mb: 0.5, color: "#374151" }}>
                    • Jedna databáze uživatelů pro všechny klienty
                  </Typography>
                  <Typography variant="body2" sx={{ color: "#374151" }}>
                    • Zprávy pro uživatele, kteří nejsou přihlášeni, zůstávají ve
                    frontě a doručí se po přihlášení
                  </Typography>
                </Paper>
                <Box sx={{ mt: 3 }}>
                  <Paper
                    elevation={3}
                    sx={{
                      p: { xs: 2, md: 3 },
                      borderRadius: 3,
                      backgroundColor: "#ffffff",
                      border: "1px solid #e5e7eb",
                    }}
                  >
                    <Login setUserToken={props.setUserToken} />
                  </Paper>
                </Box>
              </Grid>
              <Grid size={{ xs: 12, md: 7 }}>
                <Paper
                  elevation={3}
                  sx={{
                    height: "100%",
                    borderRadius: 3,
                    overflow: "hidden",
                    backgroundColor: "#f3f4f6",
                  }}
                >
                  <MainPagePreview />
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