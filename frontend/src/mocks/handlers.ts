import { http, HttpResponse } from 'msw';

export const handlers = [
  http.post('/auth/login', () =>
    HttpResponse.json({ accessToken: 'mock-token', userId: 'user-1' })
  ),
  http.post('/auth/register', () => new HttpResponse(null, { status: 201 })),
  http.post('/auth/refresh', () =>
    HttpResponse.json({ accessToken: 'refreshed-token', userId: 'user-1' })
  ),
];
