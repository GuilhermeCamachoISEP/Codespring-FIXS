# Decisões Técnicas

## Backend
Escolhemos Spring Boot porque:
- Setup rápido
- Muitas libs prontas
- Fácil criar APIs REST

Arquitetura:
- Controller → recebe pedidos
- Service → lógica
- Repository → acesso à base de dados

## Frontend
React com Vite:
- Hot reload rápido
- Simples para UI moderna

## Base de dados
H2 (in-memory):
- Zero setup
- Perfeito para hackathon

## Comunicação
- REST API (JSON)

## Versionamento
- main → versão final
- dev → integração
- feature/* → desenvolvimento

## Deploy (se houver tempo)
- Backend: Render / Railway
- Frontend: Vercel