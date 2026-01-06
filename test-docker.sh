#!/bin/bash
# ChatNexus Docker Quick Test Script
# Run this to test Docker locally in 2 minutes

echo "🐳 ChatNexus Docker Test"
echo "========================"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

echo "✅ Docker found"
echo ""

# Build Docker image
echo "🔨 Building Docker image..."
docker build -t chatnexus:latest .

if [ $? -ne 0 ]; then
    echo "❌ Docker build failed!"
    exit 1
fi

echo "✅ Docker image built successfully"
echo ""

# Show image info
echo "📊 Image Information:"
docker images chatnexus:latest --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
echo ""

echo "✅ Docker setup is working!"
echo ""
echo "Next steps:"
echo "1. Deploy to Railway: Follow README.md section '🚂 Deploy to Railway'"
echo "2. Or run locally with: docker run -d -p 8080:8080 chatnexus:latest"
echo ""

