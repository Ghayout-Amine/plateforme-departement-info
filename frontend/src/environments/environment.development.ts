export const environment = {
  production: false,
  // Les appels /api passent par proxy.conf.json vers http://localhost:8080 (pas de souci de CORS)
  apiUrl: '/api',
  // TEMPORAIRE : connexion automatique avec l'utilisateur de test du backend (DataInitializer)
  // tant que la page de connexion n'existe pas. Mettre à null quand elle sera prête.
  devLogin: { email: 'test@pgdi.ma', password: 'Test1234!' } as {
    email: string;
    password: string;
  } | null,
};
