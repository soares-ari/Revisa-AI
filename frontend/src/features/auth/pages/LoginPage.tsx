import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { LoginForm } from '../components/LoginForm';

export const LoginPage = () => (
  <div className="min-h-screen flex items-center justify-center px-4">
    <div className="w-full max-w-sm">
      <p className="text-center font-bold text-xl mb-6">REVISA AI</p>
      <Card>
        <CardHeader>
          <CardTitle>Entrar</CardTitle>
        </CardHeader>
        <CardContent>
          <LoginForm />
        </CardContent>
      </Card>
    </div>
  </div>
);
