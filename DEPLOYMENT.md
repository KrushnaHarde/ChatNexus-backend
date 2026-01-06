# ChatNexus Deployment Guide for Railway & Render

## Overview

This guide covers deploying ChatNexus to **Railway.app** and **Render.com** using Docker. Both platforms support containerized applications and make deployment simple.

---

## Prerequisites

- Docker installed locally (for testing)
- GitHub repository with your ChatNexus code
- Railway or Render account

---

## Option 1: Deploy to Railway.app ✈️

### Step 1: Prepare Your Repository

1. Commit all changes including Dockerfile:
   ```bash
   git add .
   git commit -m "Add Docker configuration for Railway deployment"
   git push
   ```

2. Ensure `.env` is in `.gitignore` (it is - verified ✓)

### Step 2: Create Railway Project

1. Go to [railway.app](https://railway.app)
2. Click **"Create a new project"**
3. Select **"Deploy from GitHub repo"**
4. Connect your GitHub account
5. Select **ChatNexus** repository

### Step 3: Configure Environment Variables

In Railway Dashboard → Your Project → Variables:

```
SPRING_PROFILES_ACTIVE=prod
PROD_MONGO_URI=mongodb://mongo:YOUR_PASSWORD@mongodb.railway.internal:27017
PROD_MONGO_DATABASE=chat_nexus
PROD_JWT_SECRET=your-strong-production-secret
PROD_JWT_EXPIRATION=86400000
PROD_CLOUDINARY_CLOUD_NAME=your_cloud_name
PROD_CLOUDINARY_API_KEY=your_api_key
PROD_CLOUDINARY_API_SECRET=your_api_secret
```

**Note:** Railway automatically provides the `PORT` environment variable.

### Step 4: Configure MongoDB (if using Railway MongoDB)

1. In Railway Dashboard, create a new **MongoDB** service
2. Link it to your ChatNexus service
3. Get the connection URL from MongoDB service → Variables → MONGO_URL
4. Use this URL for `PROD_MONGO_URI`

### Step 5: Deploy

Railway automatically deploys when you push to GitHub. Check the **Deployments** tab for logs.

### Step 6: Access Your Application

- Your app URL will be shown in the Railway dashboard
- Access: `https://your-app-name.up.railway.app`

---

## Option 2: Deploy to Render.com 🎨

### Step 1: Prepare Your Repository

Same as Railway - commit your code.

### Step 2: Create Render Service

1. Go to [render.com](https://render.com)
2. Click **"New +"** → **"Web Service"**
3. Select **"Build and deploy from a Git repository"**
4. Connect GitHub and select ChatNexus repo
5. Configure:
   - **Name:** chatnexus
   - **Root Directory:** (leave empty)
   - **Environment:** Docker
   - **Plan:** Free or Paid (as needed)

### Step 3: Configure Build & Deploy Settings

In Render Dashboard:
- **Dockerfile Path:** `./Dockerfile`
- **Docker Command:** (leave empty - uses ENTRYPOINT)

### Step 4: Add Environment Variables

In Render Dashboard → Environment:

```
SPRING_PROFILES_ACTIVE=prod
PROD_MONGO_URI=mongodb://mongo:YOUR_PASSWORD@mongodb-uri.mongocluster.cosmos.azure.com/
PROD_MONGO_DATABASE=chat_nexus
PROD_JWT_SECRET=your-strong-production-secret
PROD_JWT_EXPIRATION=86400000
PROD_CLOUDINARY_CLOUD_NAME=your_cloud_name
PROD_CLOUDINARY_API_KEY=your_api_key
PROD_CLOUDINARY_API_SECRET=your_api_secret
PORT=8080
```

### Step 5: Deploy

Click **"Create Web Service"** - Render will build and deploy automatically.

### Step 6: Access Your Application

- Your app URL: `https://chatnexus.onrender.com`

---

## Important Configuration Notes

### MongoDB URLs

| Environment | URL Format | When to Use |
|------------|-----------|-----------|
| **Development (Local)** | `mongodb://mongo:password@turntable.proxy.rlwy.net:16373` | Running locally on your machine |
| **Production (Railway)** | `mongodb://mongo:password@mongodb.railway.internal:27017` | Deployed ON Railway (internal network) |
| **Production (Render)** | `mongodb://user:pass@host.mongodb.net/dbname` | Using MongoDB Atlas or similar |

**Important:** Use `mongodb.railway.internal` ONLY when deployed on Railway. For local development, use the public URL.

### JWT Secret Generation

For production, generate a strong secret:

**Option 1: Using OpenSSL**
```bash
openssl rand -hex 32
```

**Option 2: Using Python**
```python
import secrets
print(secrets.token_hex(32))
```

**Option 3: Using Node.js**
```bash
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

---

## Building & Testing Locally with Docker

### Build Docker Image

```bash
docker build -t chatnexus:latest .
```

### Run with Docker Compose (includes MongoDB)

```bash
# Copy your .env file
cp .env.example .env
# Fill in your actual values

# Start services
docker-compose up -d

# View logs
docker-compose logs -f chatnexus

# Stop services
docker-compose down
```

### Run Standalone Docker Container

```bash
docker run \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e PROD_MONGO_URI=mongodb://... \
  -e PROD_JWT_SECRET=... \
  -e PROD_CLOUDINARY_CLOUD_NAME=... \
  -e PROD_CLOUDINARY_API_KEY=... \
  -e PROD_CLOUDINARY_API_SECRET=... \
  chatnexus:latest
```

---

## Troubleshooting

### Issue: "Cannot connect to MongoDB"

**Solution:**
- Verify the correct MongoDB URI for your environment
- Check username/password are correct
- Ensure MongoDB is running (if using local MongoDB)
- For Railway: Use internal URL ONLY if deployed on Railway

### Issue: "Port already in use"

**Solution:**
- Docker will map to a different port on Railway/Render (automatic)
- Locally: Change port mapping: `docker run -p 9090:8080 ...`

### Issue: "Health check failing"

**Solution:**
- Wait 40+ seconds for app startup (see HEALTHCHECK in Dockerfile)
- Check logs: `docker logs chatnexus-app`
- Verify Actuator endpoint: `/actuator/health`

### Issue: "Cloudinary upload fails"

**Solution:**
- Verify API key and secret in environment variables
- Check cloud name matches your Cloudinary account
- Ensure Cloudinary credentials haven't expired

### Issue: "Application won't start"

**Solution:**
- Check logs: `docker-compose logs chatnexus`
- Verify all environment variables are set
- Ensure MongoDB is accessible
- Check JWT secret is properly set

---

## Security Best Practices

✅ **DO:**
- Use strong, unique secrets for production
- Enable HTTPS (Railway/Render handle this automatically)
- Rotate credentials periodically
- Use production-only MongoDB users
- Never commit `.env` files

❌ **DON'T:**
- Use the same secret for dev and production
- Share your `.env` file
- Commit sensitive data to GitHub
- Use weak passwords for MongoDB

---

## Deployment Checklist

- [ ] Dockerfile created and tested locally
- [ ] `.dockerignore` configured
- [ ] `docker-compose.yml` created for local testing
- [ ] `.env` file created (not committed)
- [ ] `.env.example` created as template
- [ ] Repository pushed to GitHub
- [ ] Railway/Render project created
- [ ] Environment variables configured
- [ ] MongoDB deployed or external service configured
- [ ] Health check endpoint working
- [ ] Application accessible at deployment URL

---

## Next Steps

1. **Test locally first:** `docker-compose up`
2. **Deploy to Railway or Render**
3. **Monitor logs** and health status
4. **Configure custom domain** (optional)
5. **Set up CI/CD** for automatic deployments

---

## Useful Commands

### Docker

```bash
# Build image
docker build -t chatnexus:latest .

# List images
docker images

# Run container
docker run -d -p 8080:8080 chatnexus:latest

# View logs
docker logs -f <container_id>

# Stop container
docker stop <container_id>

# Remove image
docker rmi chatnexus:latest
```

### Docker Compose

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Rebuild and restart
docker-compose up -d --build
```

### Railway CLI (optional)

```bash
# Install
npm install -g @railway/cli

# Login
railway login

# Deploy
railway up

# View logs
railway logs
```

---

## Support

- **Railway Docs:** https://docs.railway.app/
- **Render Docs:** https://render.com/docs
- **Docker Docs:** https://docs.docker.com/
- **Spring Boot Docs:** https://spring.io/projects/spring-boot

