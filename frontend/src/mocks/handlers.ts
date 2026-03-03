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
];
