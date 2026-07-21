# 🚀 Product API – Backend con Chat en Tiempo Real

![Java](https://img.shields.io/badge/Java-17-blue?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-green?logo=spring)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)
![Docker](https://img.shields.io/badge/Docker-✓-blue?logo=docker)
![CI/CD](https://img.shields.io/badge/CI/CD-✓-brightgreen)

**Backend profesional** para gestión de productos con **autenticación JWT**, **roles** y **chat en tiempo real** (WebSockets). Totalmente contenerizado y con integración continua.

---

## ✨ Lo que incluye

- 🔐 Autenticación JWT (access/refresh tokens)
- 👥 Roles (USER/ADMIN)
- 📡 API REST paginada y documentada (Swagger)
- 💬 Chat en vivo + notificaciones automáticas (WebSocket)
- 📊 Logs JSON (listos para monitoreo)
- ✅ Tests unitarios, integración y WebSocket
- 🐳 Docker Compose (desarrollo y despliegue)
- ⚙️ CI/CD con GitHub Actions

---

## 🧰 Stack

**Java 17 · Spring Boot 4.1 · MySQL 8.0 · WebSocket · JWT · Docker · GitHub Actions**

---

## 🚀 Ejecutar en local (1 minuto)

```bash
git clone https://github.com/Alcardro/product-api.git
cd product-api
docker-compose up --build

Accede a:
API: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui.html
Demo chat: http://localhost:8080/ws-test.html
Usuario admin por defecto: admin / admin123


Endpoints principales
Método	Endpoint	Descripción	Autenticación
POST	/api/auth/login	Obtener token JWT	Pública
GET	/api/products	Listar productos (paginado)	Requerida
POST	/api/products	Crear producto	Admin
DELETE	/api/products/{id}	Eliminar producto	Admin
WS	/ws	Conexión WebSocket (STOMP)	Token en URL

Tests y CI/CD
bash
mvn test
GitHub Actions ejecuta todos los tests (unitarios, integración, WebSocket) en cada push a main.


👤 Autor
Alcardro – GitHub


