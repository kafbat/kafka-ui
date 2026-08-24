# Subject Strategy Demo
## Run Locally
1. From root run:
   ```shell
   ./gradlew :api:build -Pinclude-frontend=true -x test
   ```
2. From `documentation/compose/subject-strategy-demo` run:
   ```shell
   docker compose up --build -d
   # to stop run
   docker compose down -v
   ```
3. Open http://localhost:8080/
4. Play around with producing and reproducing messages