#!/bin/bash

# Connect to Minikube's Docker daemon
eval $(minikube -p minikube docker-env)

echo "Stopping Running Container $(docker stop kubeapi 2>/dev/null || true)"
echo "Deleting Existing Container $(docker rm kubeapi 2>/dev/null || true)"

# Build the application
mvn clean install -DskipTests
mkdir -p target/dependency
cd target/dependency && jar -xf ../*.jar && cd -

# Tag and build Docker image
TIMESTAMP=$(date +"%Y%m%d%H%M%S")
echo "Tagging Old Binary with: $TIMESTAMP"

docker tag kubeapi:latest kubeapi:$TIMESTAMP 2>/dev/null || true
docker build -t kubeapi:latest .

# Apply k8s config and force restart
kubectl delete -f ./kubernetes/deployment.yaml
kubectl apply -f ./kubernetes/deployment.yaml