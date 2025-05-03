#!/bin/bash

echo "🔄 Resetting local Git state..."
git fetch origin
git reset --hard origin/master
git clean -fd

echo "📥 Pulling latest code from master..."
git pull origin master

echo "🧼 Stopping and removing existing Docker containers..."
docker-compose down

echo "🔧 Rebuilding Docker images..."
docker-compose build

echo "🚀 Starting Docker containers in the foreground (to support --reload for FastAPI)..."
docker-compose up
