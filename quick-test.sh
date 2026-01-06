#!/bin/bash
# Simple command to test Docker deployment locally

echo "Testing Docker Build..."
docker build -t chatnexus:latest . && echo "✅ Docker build successful!" || echo "❌ Docker build failed!"

echo ""
echo "Image created. To run locally:"
echo "docker run -d -p 8080:8080 chatnexus:latest"
echo ""
echo "Then check: http://localhost:8080/actuator/health"

