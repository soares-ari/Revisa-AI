import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { GoogleButton } from '../components/GoogleButton';

export const LandingPage = () => (
  <div className="min-h-screen flex flex-col">
    <header className="flex items-center justify-between px-6 py-4 border-b">
      <span className="font-bold text-lg">REVISA AI</span>
      <div className="flex gap-2">
        <Button variant="ghost" asChild>
          <Link to="/login">Entrar</Link>
        </Button>
        <Button asChild>
          <Link to="/register">Cadastrar</Link>
        </Button>
      </div>
    </header>

    <main className="flex-1 flex flex-col items-center justify-center px-4 text-center gap-6">
      <h1 className="text-4xl font-bold max-w-xl">
        Estude mais inteligente. Seja aprovado mais rápido.
      </h1>
      <p className="text-muted-foreground max-w-md">
        IA que aprende com seus erros e foca no que realmente cai nas provas de
        CEBRASPE, FGV e CESGRANRIO.
      </p>
      <div className="flex flex-col gap-3 w-full max-w-xs">
        <Button size="lg" asChild>
          <Link to="/register">Começar gratuitamente</Link>
        </Button>
        <GoogleButton />
      </div>
    </main>

    <section className="border-t py-8 flex justify-center gap-12 text-sm text-muted-foreground">
      <span>+10.000 questões</span>
      <span>3 bancas</span>
      <span>IA Claude</span>
    </section>

    <section className="border-t py-8 px-6">
      <h2 className="text-center font-semibold mb-6">Como funciona</h2>
      <div className="flex justify-center gap-8 flex-wrap text-sm text-muted-foreground">
        <div className="text-center">
          <p className="font-medium text-foreground">1. Escolha a banca</p>
          <p>CEBRASPE, FGV ou CESGRANRIO</p>
        </div>
        <div className="text-center">
          <p className="font-medium text-foreground">2. Estude</p>
          <p>Questões reais com explicações da IA</p>
        </div>
        <div className="text-center">
          <p className="font-medium text-foreground">3. Evolua</p>
          <p>Acompanhe seu desempenho</p>
        </div>
      </div>
    </section>
  </div>
);
