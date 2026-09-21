export const environment = {
  production: true,
  // Relative path: nginx (see frontend/nginx.conf) proxies /api/* to the
  // backend container, so the browser only ever talks to one origin and
  // CORS never comes into play in production. If you deploy the frontend
  // and backend on separate domains instead, replace this with the
  // backend's absolute URL, e.g. 'https://api.yourdomain.com/api/support'.
  apiUrl: '/api/support'
};
