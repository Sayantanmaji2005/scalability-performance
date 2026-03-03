# Render Deployment - Tasks

## Current Task
Update Spring configs for Render-friendly env handling

## Tasks
- [x] 1. Update backend-api/application.yml - improve Kafka bootstrap-servers and DB URL fallback handling
- [x] 2. Update worker-service/application.yml - improve Kafka bootstrap-servers handling
- [x] 3. Enhance RENDER_DEPLOY.md with one-click dashboard deployment instructions

## Completion Criteria
- [x] All Spring Boot services properly handle Render environment variables
- [x] Kafka bootstrap-servers can be set via standard SPRING_KAFKA_BOOTSTRAP_SERVERS env var
- [x] Database URL fallback works correctly for PostgreSQL connection strings
- [x] Documentation allows one-click deployment from Render dashboard

