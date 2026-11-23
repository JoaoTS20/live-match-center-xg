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
