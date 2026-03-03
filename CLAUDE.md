# Revisa AI — CLAUDE.md

## Visão Geral do Projeto
Plataforma de estudos para concursos públicos com IA. Permite que candidatos
pratiquem questões de provas anteriores de CEBRASPE/CESPE, FGV e CESGRANRIO,
com explicações geradas pelo Claude Haiku 4.5 e dashboard de desempenho.

**Stack:** Java 21 + Spring Boot 3 (backend) · React 19 + TypeScript (frontend)
**Banco:** MongoDB Atlas (produção) · MongoDB via Docker (local)
**Deploy:** Railway (backend) · Vercel (frontend)
**CI/CD:** GitHub Actions
**Metodologia:** TDD — testes sempre antes da implementação

---

## Estrutura do Monorepo

```
revisa-ai/
├── backend/
│   ├── src/
│   │   ├── main/java/com/revisaai/
│   │   │   ├── auth/
│   │   │   ├── ingestion/
│   │   │   ├── question/
│   │   │   ├── study/
│   │   │   ├── explanation/
│   │   │   ├── user/
│   │   │   └── shared/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-test.yml
│   └── test/
├── frontend/
│   ├── src/
│   │   ├── components/ui/       # Shadcn/UI — não editar diretamente
│   │   ├── features/
│   │   │   ├── auth/
│   │   │   ├── study/
│   │   │   ├── dashboard/
│   │   │   └── ingestion/
│   │   ├── hooks/
│   │   ├── lib/
│   │   └── types/
├── .github/workflows/
├── docker-compose.yml
└── CLAUDE.md
```

---

## Regras de Desenvolvimento

### Obrigatórias — nunca violar
- **TDD estrito:** escreva o teste antes da implementação, sem exceções
- Nenhum serviço Spring é implementado sem teste unitário correspondente
- Nenhum componente React com lógica complexa sem teste no Vitest
- Commits pequenos e atômicos após cada feature funcionando e testada
- Nunca commitar credenciais, segredos ou arquivos .env
- Variáveis de ambiente sempre via .env local ou secrets do GitHub Actions

### Backend
- Java 21 com records, sealed classes e pattern matching onde aplicável
- Spring Boot 3 — use @RestController, @Service, @Repository consistentemente
- Autenticação stateless com JWT — nunca session-based
- Respostas da API sempre encapsuladas em ResponseEntity<>
- Tratamento de erros centralizado via @ControllerAdvice
- Logs com SLF4J — nunca System.out.println()
- Testes unitários com JUnit 5 + Mockito
- Testes de integração com Testcontainers (MongoDB real, sem mocks de banco)
- Testes de controller com MockMvc

### Frontend
- TypeScript strict mode — sem any implícito
- Cada feature é autocontida: components, hooks, types e services próprios
- TanStack Query para todo estado servidor — sem useEffect para fetch
- React Hook Form + Zod para todos os formulários
- Shadcn/UI como base de componentes — Tailwind v4 para customização
- Framer Motion para transições entre estados na tela de estudo
- Testes com Vitest + Testing Library
- MSW para interceptar chamadas HTTP nos testes
- Zustand para estado global de UI (tema e sessão de estudo em andamento)
  — não usar para estado servidor, apenas para estado client-side global

### Estilo de código
- Português para nomes de domínio (Questão, Sessão, Banca)
- Inglês para código técnico (controllers, services, repositories)
- Sem comentários óbvios — o código deve ser autoexplicativo
- Máximo 300 linhas por arquivo — refatore se ultrapassar

---

## Arquitetura de Autenticação
- Dois provedores: email/senha e Google OAuth2
- O backend emite JWT próprio independente do provedor
- Access token: 15 minutos · Refresh token: 7 dias (cookie httpOnly)
- O frontend nunca manipula o token do Google diretamente
- Endpoint de callback Google: /login/oauth2/code/google (Spring Security automático)

---

## Módulos e Responsabilidades

### auth
Registro email/senha, login, Google OAuth2, emissão e validação de JWT,
refresh token. Entidade: User.

### ingestion
Pipeline de processamento de PDFs de provas oficiais em duas etapas:

**Etapa 1 (implementada):** Endpoint `POST /ingestion/jobs` (JWT obrigatório) recebe
dois documentos — prova e gabarito — cada um como arquivo PDF via multipart ou URL
pública. Faz download via WebClient quando necessário, extrai texto bruto com Apache
PDFBox 3.x e persiste `IngestionJob` com `status=COMPLETED` (ou `FAILED` se houver erro).

**Etapa 2 (implementada):** Após a extração de texto, o `IngestionService` chama a API
da Anthropic (Claude Haiku 4.5, modelo `claude-haiku-4-5-20251001`) com uma única
requisição síncrona enviando `textProva` e `textGabarito`. Claude retorna um array JSON
de questões; o `QuestionParserService` parseia e persiste cada questão. Pipeline
síncrono — ocorre na mesma requisição do upload, sem @Async nesta fase.

**Validação de questões:** gabarito fora das alternativas ou dificuldade desconhecida →
persistidas com `valid=false` e `validationError` descritivo. O job ainda termina
COMPLETED; `questoesInvalidas` reflete o total no body da resposta.

**Entidade IngestionJob:**
- id, banca (enum Banca), ano? (Integer), cargo? (String)
- textProva, textGabarito — textos brutos extraídos dos PDFs
- status: PENDING → COMPLETED | FAILED
- errorMessage? — mensagem de erro quando status=FAILED
- questoesSalvas (int) — questões válidas persistidas na coleção `questions`
- questoesInvalidas (int) — questões com `valid=false` (gabarito/dificuldade inválidos)
- createdAt (@CreatedDate), updatedAt (@LastModifiedDate)

**Entidade Question — campos adicionados:**
- `boolean valid` (default `true`) — `false` quando gabarito ou dificuldade inválidos
- `String validationError` (opcional) — descreve o motivo da invalidação

**Novos componentes:**
- `QuestionParserService` — @Service; chama Anthropic, parseia JSON, salva Questions
- `AnthropicConfig` — @Configuration; @Bean `AnthropicClient` com timeout de 120s

**Requisição `POST /ingestion/jobs` (multipart/form-data):**
- `banca` (obrigatório), `ano` (opcional), `cargo` (opcional)
- Para prova: `provaArquivo` (MultipartFile) **ou** `provaUrl` (String) — ao menos um
- Para gabarito: `gabaritoArquivo` (MultipartFile) **ou** `gabaritoUrl` (String) — ao menos um

**Dependências:** `org.apache.pdfbox:pdfbox:3.0.3` · `spring-boot-starter-webflux`
(WebClient) · `com.anthropic:anthropic-java:2.15.0`

**Configuração:** `spring.servlet.multipart.max-file-size=20MB` e `max-request-size=20MB`

### question
CRUD e consulta de questões com filtros por banca, área, ano e dificuldade.
Entidade: Question.

### study
Gerencia sessões de estudo. Uma `StudySession` é criada com filtros (banca, área,
quantidade, modo) e contém uma lista ordenada de questões selecionadas aleatoriamente
do banco via `$sample`. O usuário responde uma questão por vez; cada resposta é
registrada como `Answer` embutida na sessão. `POST .../finish` finaliza a sessão e
calcula o resultado geral e o desempenho por área.

**Modos:** `ESTUDO` — gabarito revelado imediatamente após cada resposta.
`SIMULADO` — gabarito revelado apenas ao finalizar.

**Entidade StudySession** (`study_sessions`):
- id, userId (extraído do JWT), banca? (Banca), areas? (List<String>)
- quantidade (int), modo (enum: ESTUDO, SIMULADO)
- status (enum: EM_ANDAMENTO, FINALIZADA)
- questionIds (List<String>), currentIndex (int — questões respondidas até agora)
- answers (List<Answer> embutida), resultado (Resultado embutido, preenchido no finish)
- createdAt (@CreatedDate), updatedAt (@LastModifiedDate)

**Answer** (embutida em StudySession — não coleção separada):
- questionId, respostaUsuario (String), correta (boolean)
- area (String — copiada da Question para facilitar agregação)

**Resultado** (embutido — preenchido ao finalizar):
- total (int), acertos (int), percentual (double)
- desempenhoPorArea (Map<String, Double> — área → percentual)

**Endpoints** (todos JWT obrigatório):
- `POST /study/sessions` — body: banca?, areas?, quantidade (default 20), modo (default ESTUDO).
  Seleciona `quantidade` questões válidas aleatórias com os filtros. Retorna 201 com a sessão.
  Retorna 400 se não houver questões suficientes.
- `GET /study/sessions/:id` — retorna sessão. 403 se userId ≠ JWT. 404 se não encontrada.
- `POST /study/sessions/:id/answer` — body: questionId, resposta. Valida que questionId pertence
  à sessão e que status=EM_ANDAMENTO. Incrementa currentIndex. Retorna Answer; em ESTUDO inclui
  gabarito correto, em SIMULADO não. Retorna 400 se questão já respondida ou sessão FINALIZADA.
- `POST /study/sessions/:id/finish` — finaliza (status FINALIZADA), calcula resultado.
  Retorna sessão completa com resultado.

**Componentes:**
- `StudyService` — @Service; usa `MongoTemplate` para `$sample` com filtros
- `StudyController` — extrai userId de `Authentication.getName()`

**DTOs:** `CreateSessionRequest`, `AnswerRequest`, `AnswerResponse`

### explanation
Gera explicações para questões via Claude Haiku 4.5 sob demanda. A explicação é
gerada uma única vez e cacheada na coleção `explanations` do MongoDB — chamadas
subsequentes para o mesmo `questionId` retornam o cache sem chamar a Anthropic.

**Entidade Explanation** (`explanations`):
- id, questionId (String, @Indexed unique), texto (String — explicação gerada)
- createdAt (@CreatedDate)

**Endpoint** (JWT obrigatório):
- `GET /explanations/:questionId` — retorna a explicação da questão. Se já existir
  no cache, retorna diretamente. Se não existir, busca a `Question` pelo questionId
  (404 se não encontrada), gera a explicação via Claude Haiku 4.5 e persiste antes
  de retornar. Retorna 200 com a `Explanation`.

O prompt enviado ao Claude inclui o enunciado, as alternativas e o gabarito da
questão. Solicita: justificativa geral da questão, explicação de por que o gabarito
está correto, e explicação de por que cada alternativa incorreta está errada.
Resposta em texto corrido, em português, sem markdown.

**Componentes:**
- `ExplanationService` — @Service; verifica cache, chama Anthropic se necessário,
  persiste e retorna. Reutiliza bean `AnthropicClient` de `ingestion/AnthropicConfig`.
- `ExplanationRepository` — extends MongoRepository; método `findByQuestionId(String)`
  retornando `Optional<Explanation>`
- `ExplanationController` — @RestController com `GET /explanations/:questionId`

**Tratamento de erros:** `QuestionNotFoundException` já existe — reutilizada para 404.
Se a chamada à Anthropic falhar, a exceção propaga (falhas não são cacheadas).

### user
Expõe endpoints de perfil e histórico de desempenho agregado. Não cria nova entidade
nem nova coleção — agrega dados existentes da coleção `study_sessions` via
`StudySessionRepository`.

**Endpoints** (ambos com JWT obrigatório):
- `GET /users/me/stats` — retorna estatísticas globais: total de questões respondidas,
  percentual global de acertos, total de sessões finalizadas, e desempenho por área
  (média do percentual de acerto por área, calculada entre as sessões que contêm
  aquela área — não dividida pelo total de sessões).
- `GET /users/me/history` — retorna lista das sessões finalizadas em ordem decrescente
  de `createdAt`, com campos: id, banca, areas, quantidade, modo, resultado, createdAt.

**DTOs de resposta:**
- `UserStatsResponse` — record com totalQuestoes (int), percentualAcertos (double),
  totalSessoes (int), desempenhoPorArea (Map<String, Double>)
- `SessionSummary` — record com id, banca, areas, quantidade, modo, resultado, createdAt

**Regras de agregação em `UserStatsResponse` (apenas sessões FINALIZADAS):**
- `totalQuestoes` — soma de `answers.size()` de todas as sessões
- `percentualAcertos` — (respostas corretas / totalQuestoes) × 100, ou 0.0 se sem sessões
- `totalSessoes` — count de sessões FINALIZADAS
- `desempenhoPorArea` — lê de `resultado.getDesempenhoPorArea()` de cada sessão;
  agrupa por área e calcula a média. Divisor = número de sessões que contêm aquela área.

**Componentes:**
- `UserService` — @Service; usa `StudySessionRepository` com métodos derivados
  `findByUserIdAndStatus` e `findByUserIdAndStatusOrderByCreatedAtDesc`
- `UserController` — @RestController; userId extraído de `authentication.getName()`

---

## Coleções MongoDB

```
users             → perfil e credenciais
questions         → questões com metadados
study_sessions    → sessões com respostas e resultados
explanations      → cache de explicações geradas
ingestion_jobs    → rastreamento de jobs de ingestão
```

---

## Variáveis de Ambiente

### Backend (.env)
```
MONGODB_URI=mongodb://localhost:27017/revisaai
JWT_SECRET=
JWT_EXPIRATION=900000
REFRESH_TOKEN_EXPIRATION=604800000
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
ANTHROPIC_API_KEY=
```

### Frontend (.env.local)
```
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=
```

---

## API — Endpoints Principais

```
POST   /auth/register
POST   /auth/login
POST   /auth/refresh
GET    /oauth2/callback/google         ← gerenciado pelo Spring Security

GET    /questions?banca=&area=&ano=
GET    /questions/:id

POST   /ingestion/jobs                 ← multipart/form-data, JWT obrigatório

POST   /study/sessions
GET    /study/sessions/:id
POST   /study/sessions/:id/answer
POST   /study/sessions/:id/finish

GET    /explanations/:questionId

GET    /users/me/stats
GET    /users/me/history
```

---

## Docker Compose — Ambiente Local
Quatro serviços: backend (8080), frontend (5173), mongodb (27017),
mongo-express (8081) para visualização do banco durante desenvolvimento.

---

## GitHub Actions — Pipelines

### backend-ci.yml (push main + PRs)
build Maven → testes unitários → testes integração Testcontainers →
build imagem Docker → push GitHub Container Registry → deploy Railway

### frontend-ci.yml (push main + PRs)
install → lint ESLint → testes Vitest → build → deploy Vercel CLI

### ingestion-cron.yml (semanal)
Verifica novos PDFs nas fontes públicas e dispara pipeline de ingestão.

---

## Wireframes das Telas

### 1. Landing Page
```
┌─────────────────────────────────────────────────────┐
│  REVISA AI                    [Entrar]  [Cadastrar] │
├─────────────────────────────────────────────────────┤
│                                                     │
│         Estude mais inteligente.                    │
│         Seja aprovado mais rápido.                  │
│                                                     │
│    IA que aprende com seus erros e foca no          │
│    que realmente cai nas provas de CEBRASPE,        │
│    FGV e CESGRANRIO.                                │
│                                                     │
│         [Começar gratuitamente]                     │
│         [Entrar com Google]                         │
│                                                     │
├─────────────────────────────────────────────────────┤
│  📊 +10.000 questões   🏦 3 bancas   🤖 IA Claude  │
├─────────────────────────────────────────────────────┤
│  Como funciona                                      │
│  [1. Escolha a banca]  [2. Estude]  [3. Evolua]    │
└─────────────────────────────────────────────────────┘
```

### 2. Login / Cadastro
```
┌─────────────────────────────────────────────────────┐
│                    REVISA AI                        │
├─────────────────────────────────────────────────────┤
│   ┌─────────────────────────────────────────┐      │
│   │  [G]  Continuar com Google              │      │
│   │  ─────────────  ou  ─────────────       │      │
│   │  Email  [_____________________________] │      │
│   │  Senha  [_____________________________] │      │
│   │                [Entrar]                 │      │
│   │  Não tem conta? Cadastre-se             │      │
│   │  Esqueceu a senha?                      │      │
│   └─────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────┘
```

### 3. Dashboard
```
┌─────────────────────────────────────────────────────┐
│  REVISA AI          Olá, [nome] 👋       [Avatar]  │
├──────────┬──────────────────────────────────────────┤
│  🏠 Home │  Seu progresso                           │
│  📚 Estudar  ┌──────────┐ ┌──────────┐ ┌────────┐  │
│  📊 Stats │  │ questões │ │ acertos% │ │sessões │  │
│  ⚙️ Config│  └──────────┘ └──────────┘ └────────┘  │
│          │                                          │
│          │  Atividade — últimos 30 dias             │
│          │  [heatmap de sessões — Recharts]         │
│          │                                          │
│          │  Desempenho por área                     │
│          │  Informática          ████████ 84%       │
│          │  Dir. Constitucional  ██████   61%       │
│          │  Matemática           █████    55%       │
│          │  Português            ███████  72%       │
│          │                                          │
│          │  [Nova sessão de estudos →]              │
└──────────┴──────────────────────────────────────────┘
```

### 4. Configuração de Sessão
```
┌─────────────────────────────────────────────────────┐
│  ← Voltar          Nova Sessão                      │
├─────────────────────────────────────────────────────┤
│  Banca                                              │
│  ○ CEBRASPE/CESPE  ○ FGV  ○ CESGRANRIO  ○ Todas   │
│                                                     │
│  Área de conhecimento                               │
│  ☑ Informática  ☑ Português  ☐ Matemática          │
│  ☐ Dir. Constitucional  ☐ Dir. Administrativo      │
│                                                     │
│  Quantidade   ○ 10  ● 20  ○ 30  ○ 50               │
│                                                     │
│  Modo                                               │
│  ● Estudo (explicação após cada questão)            │
│  ○ Simulado (resultado só no final)                 │
│                                                     │
│              [Iniciar sessão →]                     │
└─────────────────────────────────────────────────────┘
```

### 5. Modo de Estudo
```
┌─────────────────────────────────────────────────────┐
│  Questão 7 de 20        CEBRASPE · Informática     │
│  ████████████████████░░░░░░░░░░░░░  35%            │
├─────────────────────────────────────────────────────┤
│  Ano: 2023 · Cargo: Analista de TI · PF            │
│                                                     │
│  [enunciado da questão]                             │
│                                                     │
│  ┌─────────────────┐   ┌─────────────────┐         │
│  │    ✓ CERTO      │   │    ✗ ERRADO      │         │
│  └─────────────────┘   └─────────────────┘         │
└─────────────────────────────────────────────────────┘

--- após responder ---

┌─────────────────────────────────────────────────────┐
│  ✅ Você acertou!  /  ❌ Você errou!                │
├─────────────────────────────────────────────────────┤
│  💡 Explicação (gerada pelo Claude)   [▼ expandir] │
│  ┌─────────────────────────────────────────────┐   │
│  │ Justificativa geral da questão...           │   │
│  │ CERTO  — ✅ porque...                       │   │
│  │ ERRADO — ❌ porque...                       │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  Transição animada via Framer Motion (slide down)  │
│              [Próxima questão →]                    │
└─────────────────────────────────────────────────────┘
```

### 6. Resultado da Sessão
```
┌─────────────────────────────────────────────────────┐
│               Sessão concluída! 🎉                  │
├─────────────────────────────────────────────────────┤
│                    15 / 20                          │
│                ████████████░░░  75%                 │
│                                                     │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐        │
│  │  ✅ 15   │   │  ❌  5   │   │  ⏱ 18min │        │
│  │ acertos  │   │  erros   │   │  duração │        │
│  └──────────┘   └──────────┘   └──────────┘        │
│                                                     │
│  Desempenho por área nesta sessão                   │
│  Informática   90% ████████████░                   │
│  Português     60% ████████░░░░░                   │
│                                                     │
│  Questões que merecem revisão                       │
│  Q3  · Redes      · Ver questão →                  │
│  Q11 · Português  · Ver questão →                  │
│  Q14 · Português  · Ver questão →                  │
│                                                     │
│  [Nova sessão]          [Ver dashboard]             │
└─────────────────────────────────────────────────────┘
```

### Notas de UX para implementação
- Tela 5 é o coração do produto — usar Framer Motion com slide suave
  revelando explicação após resposta
- Card de explicação começa recolhido, expande ao clicar
- Cores de feedback: acerto #22c55e · erro #ef4444
- Heatmap do dashboard referencia GitHub contributions graph
- Sidebar fixa no dashboard, oculta em mobile (menu hamburger)

---

## Status de Implementação

### Backend — concluído
- [x] Módulo auth (TDD)
- [x] Módulo question (TDD)
- [x] Módulo ingestion (TDD) — Etapa 1 e Etapa 2
- [x] Módulo study (TDD)
- [x] Módulo explanation (TDD)
- [x] Módulo user (TDD)

### Frontend — em andamento
- [x] Feature auth (landing, login, cadastro)
- [~] Feature dashboard (em andamento)
- [ ] Feature estudo

### Dívida técnica
- DashboardPage sem teste próprio — implementar junto à feature estudo

---

## Ordem de Desenvolvimento Recomendada
1. docker-compose.yml + ambiente local funcionando
2. Módulo auth (TDD) — registro, login, JWT, Google OAuth
3. Módulo question (TDD) — entidade, repositório, endpoints
4. Módulo ingestion (TDD) — pipeline PDF → Claude → MongoDB
5. Módulo study (TDD) — sessões, respostas, resultados
6. Módulo explanation (TDD) — geração e cache
7. Frontend auth — login, cadastro, Google OAuth
8. Frontend dashboard — heatmap, gráficos
9. Frontend estudo — configuração, modo de estudo, resultado
10. GitHub Actions — CI/CD completo
11. Deploy Railway + Vercel

---

## Referências Técnicas
- Spring Security OAuth2: https://docs.spring.io/spring-security/reference/servlet/oauth2
- Anthropic Java SDK: https://github.com/anthropics/anthropic-sdk-java
- Shadcn/UI: https://ui.shadcn.com
- TanStack Query: https://tanstack.com/query/latest
- Testcontainers: https://testcontainers.com/guides/testing-spring-boot-rest-api-using-testcontainers
