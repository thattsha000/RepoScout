   import { defineConfig } from 'vite'
   import react from '@vitejs/plugin-react'

   //standard way for config declaration
   export default defineConfig({
     plugins: [react()],
     server: {
      // Dev only for now: forward any request starting with /api to the Spring Boot server on port 8080.
      // Everything else (the React page, JS, CSS) is served by Vite on 5173.
       proxy: { '/api': 'http://localhost:8080' },
     },
   })
