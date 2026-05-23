# Briefing para a IA — Codespring-FIXS

Este ficheiro explica o projeto, as ideias de implementação, o ambiente de desenvolvimento e fornece um formato claro para a IA (assistente) fazer perguntas quando houver dúvidas. Está em português e foi criado para facilitar explicações rápidas à IA e para servir como fonte única de verdade.

---

## 1. Visão geral do projeto

- Nome: Codespring-FIXS
- Tipo: Aplicação web full‑stack (Spring Boot backend + Vite + React frontend)
- Objetivo: Aplicação de moda/wardrobe que permite registar/utilizadores, fazer upload de peças, gerar outfits suportados por AI e funcionalidades de onboarding e swipe.

Componentes principais:
- Backend: `backend/demo/demo` (Spring Boot)
- Frontend: `frontend/npm` (Vite + React)
- Pasta de uploads: `uploads/` (ficheiros de imagem enviados)

---

## 2. Objetivos principais

- Fornecer API REST para autenticação, gestão de guarda-roupa, upload de imagens e endpoints de AI.
- UI React para onboarding, login/registro, upload e visualização de outfits.
- Integração com modelo AI (variável `gemini.model`) para classificação e geração de outfits.

---

## 3. Arquitetura e fluxo de dados

1. O frontend chama endpoints do backend definidos em `frontend/npm/src/services/api.js` (variável `VITE_API_URL`).
2. O backend expõe endpoints REST (`/auth/*`, `/wardrobe/*`, `/onboarding/*`, `/ai/*`, `/outfits` etc.).
3. Uploads são gravados em `app.upload.dir` definido em `backend/demo/demo/src/main/resources/application.properties` (por omissão `./uploads`).
4. O backend usa H2 em memória para desenvolvimento e JPA para persistência.

---

## 4. Endpoints e contratos (resumo a partir do frontend)

- POST /auth/register — registar utilizador (email, password, name)
- POST /auth/login — login (email, password) → retorna token
- POST /onboarding/styles — gravar preferências de estilos
- GET /onboarding/status — estado do onboarding
- POST /onboarding/swipe — salvar swipe (imageId, liked)
- POST /wardrobe/upload — upload de peça (multipart/form-data)
- GET /wardrobe[?category] — listar peças
- DELETE /wardrobe/{id} — eliminar peça
- GET /wardrobe/count — contar peças
- GET /outfits[?lat&lon] — obter outfits (possivelmente baseado em clima)
- GET /weather?lat=&lon= — obter clima
- GET /ai/status — estado do serviço AI

Observação: confirmar contratos (campos de request/response) diretamente no código do backend para precisão.

---

## 5. Variáveis de ambiente e configuração

- Backend (arquivo `application.properties`):
  - `spring.datasource.*` — H2 (dev)
  - `jwt.secret`, `jwt.expiration`
  - `server.port` — configurável via `SERVER_PORT` (ex.: `SERVER_PORT=8081`)
  - `gemini.api.key` — chave API para o modelo AI
  - `gemini.model` — modelo AI a usar (ex.: `gemini-2.5-flash-lite`)
  - `app.upload.dir` — diretoria para uploads

- Frontend (Vite):
  - `VITE_API_URL` — URL base da API (ex.: `http://localhost:8081`)

Exemplo de ficheiro `.env` para desenvolvimento (colocar no `frontend/npm` e no `backend/demo/demo` conforme necessário):

```powershell
# Backend (backend/demo/demo/.env)
SERVER_PORT=8081
GEMINI_API_KEY=changeme

# Frontend (frontend/npm/.env)
VITE_API_URL=http://localhost:8081
```

---

## 6. Comandos úteis (Windows PowerShell)

Backend — testar:
```powershell
Set-Location "C:\Users\renat\IdeaProjects\Codespring-FIXS\backend\demo\demo"
.\mvnw.cmd test
```

Backend — executar em dev:
```powershell
Set-Location "C:\Users\renat\IdeaProjects\Codespring-FIXS\backend\demo\demo"
# $env:SERVER_PORT = "8081"  # opcional
.\mvnw.cmd spring-boot:run
```

Frontend — instalar dependências e dev server:
```powershell
Set-Location "C:\Users\renat\IdeaProjects\Codespring-FIXS\frontend\npm"
npm install
npm run dev
```

Frontend — build produção:
```powershell
Set-Location "C:\Users\renat\IdeaProjects\Codespring-FIXS\frontend\npm"
npm run build
```

---

## 7. Ideias e estratégias de implementação

- Autenticação
  - JWT com expiração (já presente). Garantir endpoints seguros e refresh tokens se necessário.

- Upload de imagens
  - Validar tipos e tamanhos (já limitado a 10MB no `application.properties`).
  - Armazenamento local em `uploads/` para dev; considerar S3 para produção.

- AI (classificação/geração)
  - Serviço separado ou classe `AiService` que chama API externa usando `gemini.api.key`.
  - Implementar retrys/backoff e circuit breaker (Resilience4j) para chamadas AI.

- Frontend UX
  - Onboarding com swipe já presente. Garantir cache offline mínimo (localStorage) até integração completa.

- Testes
  - Unitários: JUnit + Mockito para backend
  - Integração: Spring Boot Test com banco H2 (já presente)
  - E2E: Playwright ou Cypress para fluxo principal (login, upload, onboarding)

---

## 8. Lista de verificação para decisões abertas

Coloque aqui decisões que precisam de resposta:

1. Produção: onde vão os uploads (S3/Cloud)?
2. Autenticação: refresh tokens necessários? Política de expiração? JWT logout?
3. Resiliência da AI: qual SLA/timeout aceitável? Retries ou fallback locais?
4. Enriquecimento de imagens: precisa de thumbnailing/normalização?
5. Telemetria: definir logs/metrics (Prometheus + Grafana?)

---

## 9. Como quero que a IA (assistente) opere

1. Quando a IA não tiver informação suficiente para tomar uma decisão, deve sempre perguntar.
2. Use este ficheiro (`docs/AI_BRIEF.md`) como primeira referência.
3. Perguntas devem ser concisas e focadas. Use o template de pergunta abaixo.

Template de pergunta para a IA usar quando tiver dúvidas:

```
TÍTULO: <curtamente o objectivo da decisão/duvida>
CONTEXT: <breve contexto — ficheiros, endpoints, o que já está implementado>
OPÇÕES CONSIDERADAS: <lista curta de alternativas>
PERGUNTA: <pergunta específica para o utilizador>
IMPACTO: <o que muda se escolher X ou Y>
```

Exemplo real:

```
TÍTULO: Onde armazenar uploads em produção?
CONTEXT: Atualmente uploads locais em `app.upload.dir` (./uploads). O frontend espera URLs acessíveis publicamente.
OPÇÕES CONSIDERADAS: S3 (recomendado), armazenamento em VM, CDN + storage
PERGUNTA: Preferes S3 (AWS) para armazenar os ficheiros ou usar um serviço de object storage do provider (GCP/Azure)?
IMPACTO: S3 requer configuração de bucket/credentials e alterações nos endpoints; armazenamento em VM exige gestão de disco e backup.
```

---

## 10. Perguntas que a IA deve sempre fazer quando encontrar incertezas

1. Este endpoint está statusado/documentado? (peça-os a apontar o ficheiro de código).
2. Há validações de input já implementadas? (se não, perguntar formato esperado).
3. Devs preferem solução rápida (MVP) ou solução robusta desde o início?
4. Qual o ambiente alvo de deployment (Heroku, AWS, Azure, Docker/Kubernetes)?

---

## 11. Como podes usar este ficheiro

- Sempre que precisares de explicar o projeto a outro assistente (ou a um novo dev), aponta para `docs/AI_BRIEF.md`.
- Quando a IA tiver dúvidas, use o template de pergunta e guarde as respostas no próprio ficheiro ou num `docs/DECISIONS.md`.

---

Se quiseres, eu crio também um `docs/DECISIONS.md` para registar respostas e decisões e/ou um `.env.example` nas pastas `backend/demo/demo` e `frontend/npm`.

Fim do ficheiro.

