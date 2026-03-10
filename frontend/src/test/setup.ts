import '@testing-library/jest-dom';
import axios from 'axios';
import { server } from '@/mocks/server';

axios.defaults.adapter = 'fetch';

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
