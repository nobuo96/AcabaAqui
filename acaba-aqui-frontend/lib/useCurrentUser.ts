import { useEffect, useState } from 'react';
import { apiFetch } from './api';
import { getAuthUserId } from './authToken';

interface CurrentUser {
  nome: string;
}

export function useCurrentUser() {
  const [nome, setNome] = useState('');
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');

  useEffect(() => {
    let active = true;

    async function carregarUsuario() {
      try {
        const userId = await getAuthUserId();
        if (!userId) {
          throw new Error('ID do usuário não encontrado na sessão. Entre novamente.');
        }

        const response = await apiFetch(`/user/${encodeURIComponent(userId)}`);
        if (!response.ok) {
          throw new Error(`Não foi possível carregar o usuário (${response.status}).`);
        }

        const user: CurrentUser = await response.json();
        if (active) {
          setNome(user.nome);
        }
      } catch (error) {
        if (active) {
          setErro(error instanceof Error ? error.message : 'Não foi possível carregar o usuário.');
        }
      } finally {
        if (active) {
          setCarregando(false);
        }
      }
    }

    void carregarUsuario();
    return () => {
      active = false;
    };
  }, []);

  return { nome, carregando, erro };
}