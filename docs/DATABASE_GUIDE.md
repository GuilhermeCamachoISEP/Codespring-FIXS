# Guia da Base de Dados (H2)

Este guia explica como a equipa pode visualizar, testar e perceber a base de dados do projeto de forma muito simples durante a hackathon.

## Como funciona?
Estamos a usar a base de dados **H2** em modo de ficheiro local persistente.
Isto significa que os dados não são apagados quando o servidor desliga (ficam guardados na pasta `backend/demo/demo/data/fashiondb`).
A configuração atual garante que o Hibernate apenas *atualiza* (`update`) a base de dados sempre que houver novos campos ou entidades sem apagar nada.

> **Importante para colaboração local**: Se dois elementos da equipa correrem o backend nos seus portáteis, cada um terá a sua PRÓPRIA base de dados local isolada! Se precisarem de partilhar a mesma base de dados durante o desenvolvimento, apenas um corre o backend e o outro aponta o frontend (`API_URL`) para o IP de quem está a correr o backend.

## Aceder à Consola H2 (Interface Visual)
A consola H2 permite ver as tabelas, apagar dados e fazer *queries* diretas pelo browser.

1. Garante que o backend está a correr:
   ```bash
   cd backend/demo/demo
   ./mvnw spring-boot:run
   ```
2. Abre o teu browser e vai a: **http://localhost:8080/h2-console**
3. Preenche os campos exatamente assim:
   - **Driver Class**: `org.h2.Driver`
   - **JDBC URL**: `jdbc:h2:file:./data/fashiondb`
   - **User Name**: `sa`
   - **Password**: *(deixar em branco)*
4. Clica em **Connect**.

## Tabelas Principais (Hibernate)
No lado esquerdo do ecrã da consola, verás as seguintes tabelas (mapeadas a partir das classes Java na pasta `domain`):

- **APP_USERS**: Dados de login e conta dos utilizadores (email, password encriptada, nome).
- **USER_PREFERENCES**: Preferências guardadas no *onboarding* (pesos dos estilos escolhidos).
- **WARDROBE_ITEMS**: Todas as roupas carregadas pelo utilizador (imagem, categoria, cor, estilos, etc).
- **SWIPE_RESULTS**: Os "likes/dislikes" escolhidos na ronda de amostras do onboarding.

Podes usar o botão `Run` para fazeres um `SELECT * FROM APP_USERS;` e ver o que está lá dentro.

## Como limpar os dados? (Hard Reset)
Se a base de dados tiver dados inválidos e quiseres começar "do zero" (estado limpo), basta apagar os ficheiros que a base de dados criou.

1. Pára o servidor backend (`Ctrl + C`).
2. Apaga a pasta `data` dentro de `backend/demo/demo/`.
3. Arranca o servidor novamente. A base de dados começará completamente limpa e recriará as tabelas vazias!
