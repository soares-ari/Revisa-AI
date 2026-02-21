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
Pipeline de processamento de PDFs de provas oficiais. Usa Apache PDFBox para
extração de texto e Claude Haiku 4.5 (Batch API) para parsear questões em JSON
estruturado. Jobs rastreados em ingestion_jobs. Roda assíncrono via @Async.

### question
CRUD e consulta de questões com filtros por banca, área, ano e dificuldade.
Entidade: Question.

### study
Gerencia sessões de estudo — criação com filtros, registro de respostas,
cálculo de resultado. Entidades: StudySession, Answer.

### explanation
Gera explicações via Claude Haiku 4.5 sob demanda. Cache em MongoDB para não
reprocessar a mesma questão. Entidade: Explanation.

### user
Perfil, preferências e histórico agregado de desempenho. Entidade: UserProfile.

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
