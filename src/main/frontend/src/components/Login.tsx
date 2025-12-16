import {
  Box,
  Button,
  Chip,
  Divider,
  TextField,
  Typography,
  Alert,
} from "@mui/material";
import axios from "axios";
import React, { useState } from "react";
import { Link } from "react-router-dom";

const LOGIN_TOKEN_URL = "http://localhost:8081/login";

const Login = (props: any) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [errorText, setErrorText] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function login(e: React.FormEvent) {
    e.preventDefault();
    setErrorText(null);

    if (!username.trim() || !password.trim()) {
      setErrorText("Zadej uživatelské jméno i heslo.");
      return;
    }

    const userCredentials = {
      username: username.trim(),
      password: password.trim(),
    };

    try {
      setIsSubmitting(true);
      const response = await axios.post(LOGIN_TOKEN_URL, userCredentials);
      props.setUserToken(response.data);
    } catch (error: any) {
      try {
        const message =
          error?.response?.data || "Nepodařilo se přihlásit. Zkus to znovu.";
        setErrorText(message);
      } catch {
        setErrorText("Nelze se připojit k autentizačnímu serveru.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Box component="section">
      <Typography variant="h5" sx={{ mb: 1.5, fontWeight: 600 }}>
        Přihlášení do chatu
      </Typography>
      <Typography variant="body2" sx={{ mb: 3, opacity: 0.8 }}>
        Přihlas se pomocí účtu, který je uložen v&nbsp;společné databázi
        uživatelů.
      </Typography>

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

      <Box component="form" onSubmit={login} noValidate>
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
            autoComplete="current-password"
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
            {isSubmitting ? "Přihlašuji..." : "Přihlásit se"}
          </Button>
        </Box>
      </Box>

      <Divider sx={{ my: 2 }}>
        <Chip label="Nemáš účet?" size="small" />
      </Divider>

      <Box sx={{ textAlign: "center" }}>
        <Typography variant="body2" sx={{ mb: 1 }}>
          Zaregistruj se a vstup do hlavní místnosti.
        </Typography>
        <Button component={Link} to="/signup" variant="outlined" size="small">
          Vytvořit účet
        </Button>
      </Box>
    </Box>
  );
};

export default Login;
