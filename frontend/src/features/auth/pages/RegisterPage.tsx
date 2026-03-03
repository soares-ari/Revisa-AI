import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { RegisterForm } from '../components/RegisterForm';

export const RegisterPage = () => (
  <div className="min-h-screen flex items-center justify-center px-4">
    <div className="w-full max-w-sm">
      <p className="text-center font-bold text-xl mb-6">REVISA AI</p>
      <Card>
        <CardHeader>
          <CardTitle>Criar conta</CardTitle>
        </CardHeader>
        <CardContent>
          <RegisterForm />
        </CardContent>
      </Card>
    </div>
  </div>
);
