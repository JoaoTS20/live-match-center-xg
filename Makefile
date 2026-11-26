.PHONY: up down logs ps create-topic list-topics db-shell

up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f

ps:
	docker ps

create-topic:
	docker exec -it match-kafka /opt/kafka/bin/kafka-topics.sh --create --if-not-exists --topic match-events --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
	docker exec -it match-kafka /opt/kafka/bin/kafka-topics.sh --create --if-not-exists --topic match-commentary --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

list-topics:
	docker exec -it match-kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

db-shell:
	docker exec -it match-postgres psql -U matchuser -d matchdb


NAMESPACE := live-match-center

.PHONY: k8s-build k8s-up k8s-down k8s-status k8s-watch k8s-restart \
        k8s-logs-match k8s-logs-narrator k8s-redis-keys k8s-scale-match k8s-port-forward

# Build all 3 local app images tagged for Kubernetes
k8s-build:
	docker build -t $(NAMESPACE)/match-service:local ./match-service
	docker build -t $(NAMESPACE)/narrator-worker:local ./narrator-worker
	docker build -t $(NAMESPACE)/frontend-ui:local ./frontend-ui

# Deploy / update all manifests
k8s-up:
	kubectl apply -f k8s/

# Show pods, services, and deployments
k8s-status:
	kubectl get all -n $(NAMESPACE)

# Watch pod startup & readiness live
k8s-watch:
	kubectl get pods -n $(NAMESPACE) -w

# Tail service logs
k8s-logs-match:
	kubectl logs -f -n $(NAMESPACE) deploy/match-service

k8s-logs-narrator:
	kubectl logs -f -n $(NAMESPACE) deploy/narrator-worker

# Inspect cached match timelines in Redis
k8s-redis-keys:
	kubectl exec -n $(NAMESPACE) deploy/redis -- redis-cli KEYS "*"

# Scale match-service (override with: make k8s-scale-match REPLICAS=1)
REPLICAS ?= 2
k8s-scale-match:
	kubectl scale deployment/match-service -n $(NAMESPACE) --replicas=$(REPLICAS)

# Soft restart all workloads (keeps PostgreSQL PVC data)
k8s-restart:
	kubectl rollout restart deployment -n $(NAMESPACE)
	kubectl rollout restart statefulset -n $(NAMESPACE)

# Only needed if using NodePort instead of LoadBalancer (use 4200:4200 if Service port is 4200)
k8s-port-forward:
	kubectl port-forward -n $(NAMESPACE) svc/frontend-ui 4200:4200

# Tear down the entire stack and free RAM (also deletes PostgreSQL PVC)
k8s-down:
	kubectl delete namespace $(NAMESPACE)
