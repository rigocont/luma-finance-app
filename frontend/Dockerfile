# ---------- Compilacion ----------
FROM node:24-alpine AS build

WORKDIR /app

# Las dependencias se instalan en su propia capa: un cambio de codigo no
# invalida la cache de npm.
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund

COPY . .

# La URL de la API se decide al construir, porque Vite la inyecta en el bundle.
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ---------- Ejecucion ----------
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
