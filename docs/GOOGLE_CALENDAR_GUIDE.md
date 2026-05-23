# 📅 Guia de Integração do Google Calendar

Este documento serve para a equipa configurar e testar a funcionalidade de Sincronização do Google Calendar no projeto Codespring-FIXS.

O nosso calendário na página de *Outfits* é dinâmico e vai buscar os eventos dos próximos 7 dias diretamente à tua conta Google pessoal. Para que isto funcione localmente no computador de cada um, é necessário configurar o **Google OAuth**.

---

## Opção A (Recomendada): Usar o mesmo Client ID para toda a equipa

Se um membro da equipa já tiver criado a app na Google Cloud Console, podem usar todos a mesma chave!

**O que o dono da Chave tem de fazer:**
1. Vai a [Google Cloud Console](https://console.cloud.google.com/) > **APIs & Services** > **OAuth consent screen**.
2. Na secção **Test users**, clica em **+ ADD USERS**.
3. Adiciona os emails Google de **todos os teus colegas de grupo** (e dos Júris, se necessário).
4. Partilha o teu `Client ID` com a equipa (via Slack, Discord, WhatsApp).

**O que a Equipa tem de fazer:**
1. Abre o ficheiro `frontend/npm/src/main.jsx`.
2. Substitui o valor da variável `GOOGLE_CLIENT_ID` pela chave que o teu colega partilhou:
   ```javascript
   const GOOGLE_CLIENT_ID = "CHAVE_DO_VOSSO_COLEGA_AQUI"
   ```
3. Acede ao Frontend e faz login!

---

## Opção B: Cada membro cria o seu próprio Client ID

Se preferirem que cada um tenha a sua própria aplicação independente na Google, cada membro deve seguir estes passos:

1. Acede à [Google Cloud Console](https://console.cloud.google.com/).
2. Cria um novo Projeto.
3. Vai a **APIs & Services > Library** e pesquisa por **Google Calendar API**. Clica em **Enable** (Ativar).
4. Vai a **APIs & Services > OAuth consent screen**.
   - Escolhe **External** e preenche os campos obrigatórios.
   - Avança os passos e, na secção **Test users**, adiciona o **teu próprio email** para conseguires fazer login.
5. Vai a **APIs & Services > Credentials**.
6. Clica em **+ Create Credentials > OAuth client ID**.
7. Escolhe **Web application**.
8. Em **Authorized JavaScript origins**, clica em *Add URI* e coloca o URL exato do teu Vite local: `http://localhost:5173` (ou a porta que estiveres a usar).
9. Clica em **Create** e copia o `Client ID` fornecido (termina em `.apps.googleusercontent.com`).
10. Abre o ficheiro `frontend/npm/src/main.jsx` e cola o ID:
    ```javascript
    const GOOGLE_CLIENT_ID = "O_TEU_CLIENT_ID_AQUI"
    ```

---

## ⚠️ Resolução de Problemas Comuns

### Erro: "Access blocked: App has not completed the Google verification process" (403 access_denied)
A aplicação está em Modo de Teste. Só os emails autorizados podem fazer login.
**Solução:** Vai ao Google Cloud Console > OAuth consent screen > Test users e adiciona o teu email.

### Erro: 500 Internal Server Error ao gerar Outfit
Acontece se o Backend receber um formato de data inválido. Já corrigimos isto no `GoogleCalendar.jsx` que converte a data visual portuguesa (`23/05/2026`) para formato ISO (`2026-05-23`) antes de enviar para o Backend. Se este erro persistir, garante que não tens alterações perdidas e que fizeste `git pull`.

### O botão não faz nada
Garante que o teu Frontend está a correr exatamente no URL listado nos *Authorized JavaScript origins* (ex: se o Vite estiver no `localhost:5174` e na Google tens `5173`, a Google bloqueia o pop-up silenciosamente).
