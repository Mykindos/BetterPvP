#!/bin/bash

# Attempt to start docker daemon. (Windows)
if ! docker info >/dev/null 2>&1; then
    echo "Docker daemon is not running."
    echo "Please attempt to start Docker Desktop for the daemon to run."
    echo "Press any key to continue..."
    read -n 1 -s -r
    exit 1
fi

# Print 'Setting up environment' to the console.
echo 'Setting up environment'

# Check if docker-compose.yml file exists.
if [ -f "docker-compose.yml" ]; then
  # Run the docker-compose.yml file.
  # -d flag runs the containers in the background.
  docker-compose up -d
else
  echo "docker-compose.yml file not found in this directory."
fi

# Wait for user to press any key to continue.
echo "Press any key to continue..."
read -n 1 -s -r