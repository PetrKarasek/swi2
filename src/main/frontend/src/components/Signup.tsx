import {
  Box,
  Button,
  Chip,
  Divider,
  TextField,
  Typography,
  Alert,
  Paper,
  Container,
} from "@mui/material";
import axios from "axios";
import React, { useState } from "react";
import { Link } from "react-router-dom";

const SIGNUP_TOKEN_URL = "http://localhost:8081/signup";

const Signup = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [successText, setSuccessText] = useState<string | null>(null);
  const [errorText, setErrorText] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function signup(e: React.FormEvent) {
    e.preventDefault();
    setSuccessText(null);
    setErrorText(null);

    if (!username.trim() || !password.trim()) {
      setErrorText("Vyplň uživatelské jméno i heslo.");
      return;
    }

    const userDTO = {
      username: username.trim(),
      password: password.trim(),
    };

    try {
      setIsSubmitting(true);
      const response = await axios.post(SIGNUP_TOKEN_URL, userDTO);
      setSuccessText(response.data || "Registrace proběhla úspěšně.");
    } catch (error: any) {
      try {
        const msg =
          error?.response?.data ||
          "Registrace se nezdařila. Zkus to prosím znovu.";
        setErrorText(msg);
      } catch {
        setErrorText("Nelze se připojit k registračnímu serveru.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Container
      maxWidth="md"
      sx={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        py: { xs: 4, md: 8 },
      }}
    >
      <Paper
        elevation={8}
        sx={{
          p: { xs: 3, md: 4 },
          borderRadius: 3,
          width: "100%",
          backgroundColor: "#0f172a",
          border: "1px solid rgba(148, 163, 184, 0.35)",
        }}
      >
        <Typography variant="h5" sx={{ mb: 1.5, fontWeight: 600 }}>
          Vytvořit nový účet
        </Typography>
        <Typography variant="body2" sx={{ mb: 3, opacity: 0.8 }}>
          Registrace vytvoří uživatele ve společné databázi a automaticky jej
          přidá do hlavní veřejné chatovací místnosti.
        </Typography>

        {successText && (
          <Alert
            severity="success"
            sx={{
              mb: 2,
              borderRadius: 2,
            }}
          >
            {successText}
          </Alert>
        )}

        {errorText && (
          <Alert
            severity="error"
            sx={{
              mb: 2,
              borderRadius: 2,
            }}
          >
            {errorText}
          </Alert>
        )}

        <Box component="form" onSubmit={signup} noValidate>
          <Box sx={{ mb: 2 }}>
            <TextField
              fullWidth
              size="small"
              label="Uživatelské jméno"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
            />
          </Box>
          <Box sx={{ mb: 2 }}>
            <TextField
              fullWidth
              size="small"
              label="Heslo"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="new-password"
            />
          </Box>
          <Box sx={{ mb: 2 }}>
            <Button
              type="submit"
              variant="contained"
              color="primary"
              fullWidth
              disabled={isSubmitting}
            >
              {isSubmitting ? "Registruji..." : "Zaregistrovat se"}
            </Button>
          </Box>
        </Box>

        <Divider sx={{ my: 2 }}>
          <Chip label="Už máš účet?" size="small" />
        </Divider>

        <Box sx={{ textAlign: "center" }}>
          <Typography variant="body2" sx={{ mb: 1 }}>
            Přihlas se a začni chatovat v hlavní místnosti.
          </Typography>
          <Button component={Link} to="/" variant="outlined" size="small">
            Zpět na přihlášení
          </Button>
        </Box>
      </Paper>
    </Container>
  );
};

export default Signup;
