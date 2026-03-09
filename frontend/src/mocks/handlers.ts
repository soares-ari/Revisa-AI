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
];
