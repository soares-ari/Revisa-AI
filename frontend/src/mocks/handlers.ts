import { http, HttpResponse } from 'msw';

const BASE = 'http://localhost:8080';

export const handlers = [
  http.post(`${BASE}/auth/login`, () =>
    HttpResponse.json({ accessToken: 'mock-token', userId: 'user-1' })
  ),
  http.post(`${BASE}/auth/register`, () => new HttpResponse(null, { status: 201 })),
  http.post(`${BASE}/auth/refresh`, () =>
    HttpResponse.json({ accessToken: 'refreshed-token', userId: 'user-1' })
  ),
  http.get(`${BASE}/users/me/stats`, () =>
    HttpResponse.json({
      totalQuestoes: 150,
      percentualAcertos: 72.5,
      totalSessoes: 8,
      desempenhoPorArea: { Informática: 84, Português: 72 },
    })
  ),
  http.get(`${BASE}/users/me/history`, () =>
    HttpResponse.json([
      {
        id: 'session-1',
        banca: 'CEBRASPE',
        areas: ['Informática'],
        quantidade: 20,
        modo: 'ESTUDO',
        resultado: {
          total: 20,
          acertos: 17,
          percentual: 85.0,
          desempenhoPorArea: { Informática: 85 },
        },
        createdAt: '2025-03-01T10:00:00Z',
      },
    ])
  ),
  http.post(`${BASE}/ingestion/jobs`, () =>
    HttpResponse.json(
      {
        id: 'job-1',
        status: 'COMPLETED',
        banca: 'CEBRASPE',
        ano: 2024,
        orgao: 'PF',
        cargo: 'Analista',
        questoesSalvas: 10,
        questoesInvalidas: 2,
        errorMessage: null,
      },
      { status: 201 }
    )
  ),
  http.get(`${BASE}/ingestion/jobs/:id`, () =>
    HttpResponse.json({
      id: 'job-1',
      status: 'COMPLETED',
      banca: 'CEBRASPE',
      ano: 2024,
      orgao: 'PF',
      cargo: 'Analista',
      questoesSalvas: 10,
      questoesInvalidas: 2,
      errorMessage: null,
    })
  ),

  // Study
  http.get(`${BASE}/questions/areas`, () =>
    HttpResponse.json(['Direito Constitucional', 'Informática', 'Português'])
  ),
  http.post(`${BASE}/study/sessions`, () =>
    HttpResponse.json(
      {
        id: 'session-1',
        userId: 'user-1',
        banca: 'CEBRASPE',
        areas: ['Informática'],
        quantidade: 2,
        modo: 'ESTUDO',
        status: 'EM_ANDAMENTO',
        questionIds: ['q-1', 'q-2'],
        currentIndex: 0,
        answers: [],
        resultado: null,
        createdAt: '2026-01-01T10:00:00Z',
        updatedAt: '2026-01-01T10:00:00Z',
      },
      { status: 201 }
    )
  ),
  http.get(`${BASE}/study/sessions/:id`, () =>
    HttpResponse.json({
      id: 'session-1',
      userId: 'user-1',
      banca: 'CEBRASPE',
      areas: ['Informática'],
      quantidade: 2,
      modo: 'ESTUDO',
      status: 'EM_ANDAMENTO',
      questionIds: ['q-1', 'q-2'],
      currentIndex: 0,
      answers: [],
      resultado: null,
      createdAt: '2026-01-01T10:00:00Z',
      updatedAt: '2026-01-01T10:00:00Z',
    })
  ),
  http.get(`${BASE}/questions/:id`, () =>
    HttpResponse.json({
      id: 'q-1',
      enunciado: 'O firewall é um dispositivo de segurança que controla o tráfego de rede.',
      alternativas: ['CERTO', 'ERRADO'],
      gabarito: 'CERTO',
      area: 'Informática',
      dificuldade: 'MEDIO',
      banca: 'CEBRASPE',
      ano: 2023,
      cargo: 'Analista',
      provaId: 'prova-1',
      valid: true,
    })
  ),
  http.get(`${BASE}/explanations/:questionId`, () =>
    HttpResponse.json({
      id: 'exp-1',
      questionId: 'q-1',
      texto: 'Explicação da questão gerada pelo Claude.',
      createdAt: '2026-01-01T10:00:00Z',
    })
  ),
  http.post(`${BASE}/study/sessions/:id/answer`, () =>
    HttpResponse.json({
      questionId: 'q-1',
      respostaUsuario: 'CERTO',
      correta: true,
      gabarito: 'CERTO',
      area: 'Informática',
    })
  ),
  http.post(`${BASE}/study/sessions/:id/finish`, () =>
    HttpResponse.json({
      id: 'session-1',
      userId: 'user-1',
      banca: 'CEBRASPE',
      areas: ['Informática'],
      quantidade: 2,
      modo: 'ESTUDO',
      status: 'FINALIZADA',
      questionIds: ['q-1', 'q-2'],
      currentIndex: 2,
      answers: [
        { questionId: 'q-1', respostaUsuario: 'CERTO', correta: true, area: 'Informática' },
        { questionId: 'q-2', respostaUsuario: 'ERRADO', correta: false, area: 'Informática' },
      ],
      resultado: {
        total: 2,
        acertos: 1,
        percentual: 50.0,
        desempenhoPorArea: { Informática: 50.0 },
      },
      createdAt: '2026-01-01T10:00:00Z',
      updatedAt: '2026-01-01T10:05:00Z',
    })
  ),
];
